package org.shanoir.ng;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.shanoir.ng.email.EmailService;
import org.shanoir.ng.user.User;
import org.shanoir.ng.user.UserRepository;
import org.shanoir.ng.utils.KeycloakShanoirUtil;
import org.shanoir.ng.utils.PasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import groovyjarjarcommonscli.MissingArgumentException;

/**
 * This class does the user(s) management for Shanoir-NG in relation with
 * Keycloak and its dedicated microservice. This class reads command line
 * arguments (options) at the end of the start-up of the microservice users
 * (ShanoirUsersApplication).
 * 
 * For security reasons, we do not provide these features using a REST
 * interface. The administrator/deploy script, that runs the ms users decides
 * with additional options on the command line, what user(s) management
 * operations should be performed:
 * 
 * For code duplication reasons, we have not put this code, that interacts with:
 * - MS keycloak - EmailService - Database users into a separate .jar file (what
 * could be done), but put it directly into ms users, as ms users has already
 * access to all these resources. I decided to run up ms users differently
 * depending what the current admin wants as result.
 * 
 * This component implements the ApplicationRunner interface, what means Spring
 * Boot will call it at the end of its start-up and pre-format the command line
 * arguments as ApplicationArguments, what is better to handle than a string.
 *
 * 1) First user creation, e.g. the first initial admin user of the system, when
 * we start with a completely empty Shanoir-NG users database.
 * 
 * 2) After a migration, when all existing users have been migrated into the ms
 * users relational database, all users not yet existing in Keycloak can be
 * copied into ms keycloak, that the accounts work. Same applies for development
 * purpose: create all users from import.sql in Keycloak.
 *
 */
@Component
public class ShanoirUsersManagement implements ApplicationRunner {

	/**
	 * Logger
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ShanoirUsersManagement.class);

	/**
	 * Five values are necessary to init the Keycloak client: url, realm, client-id, login, pw
	 * Login and pw are given from the command line to avoid admin pw storage on the disk and
	 * in application.yml. URL, realm and client-id come from application.yml or env configuration.
	 * 
	 */
	private Keycloak keycloak;
	
	@Value("${keycloak.auth-server-url}")
	private String kcAdminClientServerUrl;
	
	@Value("${kc.requests.realm}")
	private String kcAdminClientRealm;
	
	@Value("${kc.requests.client.id}")
	private String kcAdminClientClientId;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private EmailService emailService;
	
	@Value("${keycloak.realm}")
	private String keycloakRealm;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (args.getOptionNames().isEmpty()) {
			LOG.info("ShanoirUsersManagement called without option. Starting up MS Users without additional operation.");
		} else {
			initKeycloakAdminClient(args);
			if(args.containsOption("createInitialAdminShanoir")) {
				/**
				 * @ToDo: implement initial admin Shanoir creation here, in database users and in keycloak, check if dbs are really empty.
				 */
			} else if (args.containsOption("syncAllUsersToKeycloak")
					&& args.getOptionValues("syncAllUsersToKeycloak").get(0) != null
					&& args.getOptionValues("syncAllUsersToKeycloak").get(0).equals("true")) {
				createUsersIfNotExisting();
			}
		}
	}

	private void initKeycloakAdminClient(ApplicationArguments args) throws MissingArgumentException {
		if(args.containsOption("kcAdminClientUsername")
				&& args.containsOption("kcAdminClientPassword")) {
			keycloak = Keycloak.getInstance(
				kcAdminClientServerUrl,
				kcAdminClientRealm,
				args.getOptionValues("kcAdminClientUsername").get(0),
				args.getOptionValues("kcAdminClientPassword").get(0),
				kcAdminClientClientId);
		} else {
			LOG.error("Missing kcAdminClientUsername and -Password");
			throw new MissingArgumentException("Missing kcAdminClientUsername and -Password");
		}
	}

	private void createUsersIfNotExisting() {
		LOG.info("Create users");
		final Iterable<User> users = userRepository.findAll();
		for (final User user : users) {
			final List<UserRepresentation> userRepresentationList = keycloak.realm(keycloakRealm).users().search(user.getUsername());
			if (userRepresentationList != null && !userRepresentationList.isEmpty()) {
				LOG.debug("User already existing in Keycloak. Do nothing.");
			} else {
				// Add user to Keycloak and save its keycloakId in the users database after
				final UserRepresentation userRepresentation = getUserRepresentation(user);
				final Response response = keycloak.realm(keycloakRealm).users().create(userRepresentation);
				final String keycloakId = KeycloakShanoirUtil.getCreatedUserId(response);
				user.setKeycloakId(keycloakId);
				userRepository.save(user);
				// Reset user password, which is stored in Keycloak only
				final CredentialRepresentation credential = new CredentialRepresentation();
				credential.setType(CredentialRepresentation.PASSWORD);
				String newPassword = PasswordUtils.generatePassword();
				credential.setValue(newPassword);
				final UserResource userResource = keycloak.realm(keycloakRealm).users().get(keycloakId);
				userResource.resetPassword(credential);
				final RoleResource roleResource = keycloak.realm(keycloakRealm).roles().get(user.getRole().getName());
				userResource.roles().realmLevel().add(Arrays.asList(roleResource.toRepresentation()));
				// Notify user
				emailService.notifyUserResetPassword(user, newPassword);
			}
		}
	}

	private UserRepresentation getUserRepresentation(final User user) {
		final Map<String, List<String>> attributes = new HashMap<String, List<String>>();
		attributes.put("userId", Arrays.asList(user.getId().toString()));
		if (user.getExpirationDate() != null) {
			attributes.put("expirationDate", Arrays.asList("" + user.getExpirationDate().getTime()));
		}
		final UserRepresentation userRepresentation = new UserRepresentation();
		userRepresentation.setAttributes(attributes);
		userRepresentation.setEmail(user.getEmail());
		userRepresentation.setEmailVerified(Boolean.TRUE);
		userRepresentation.setEnabled(user.isEnabled());
		userRepresentation.setFirstName(user.getFirstName());
		userRepresentation.setLastName(user.getLastName());
		userRepresentation.setUsername(user.getUsername());
		return userRepresentation;
	}

}