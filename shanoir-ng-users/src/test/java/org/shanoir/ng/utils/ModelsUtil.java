package org.shanoir.ng.utils;

import java.util.Date;

import org.shanoir.ng.model.Role;
import org.shanoir.ng.model.User;

/**
 * Utility class for test.
 * Generates models.
 * 
 * @author msimon
 *
 */
public class ModelsUtil {

	/**
	 * Create a role.
	 * 
	 * @return role
	 */
	public static Role createRole() {
		final Role role = new Role();
		role.setName("guest");
		role.setAccessLevel(3);
		role.setDisplayName("guest");
		return role;
	}
	
	/**
	 * Create a user.
	 * 
	 * @return user.
	 */
	public static User createUser() {
		return createUser(createRole());
	}
	
	/**
	 * Create a user with a defined role.
	 * 
	 * @param role role.
	 * @return user.
	 */
	public static User createUser(final Role role) {
		final User user = new User();
		user.setUsername("toto");
		user.setEmail("toto@to.to");
		user.setCreationDate(new Date());
		user.setRole(role);
		user.setFirstName("toto");
		user.setLastName("toto");
		return user;
	}
	
}