package org.shanoir.ng.model.error;

import java.util.List;

/**
 * Add whatever details you want here
 *
 * @author jlouis
 */
public class ErrorDetails {

	List<FormError> formErrors = null;

	/**
	 * @return the formErrors
	 */
	public List<FormError> getFormErrors() {
		return formErrors;
	}

	/**
	 * @param formErrors the formErrors to set
	 */
	public void setFormErrors(List<FormError> formErrors) {
		this.formErrors = formErrors;
	}

}