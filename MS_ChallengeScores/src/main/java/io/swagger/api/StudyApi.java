/**
 * Shanoir NG - Import, manage and share neuroimaging data
 * Copyright (C) 2009-2019 Inria - https://www.inria.fr/
 * Contact us on https://project.inria.fr/shanoir/
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/gpl-3.0.html
 */

package io.swagger.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.model.Studies;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.SpringCodegen", date = "2016-10-11T09:18:12.164Z")

@Api(value = "study", description = "the study API")
public interface StudyApi {

    @ApiOperation(value = "", notes = "Deletes all studies", response = Void.class, tags={  })
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "studies cleared", response = Void.class),
        @ApiResponse(code = 200, message = "unexpected error", response = Void.class) })
    @RequestMapping(value = "/study/all",
        produces = { "application/json" },
        method = RequestMethod.DELETE)
    ResponseEntity<Void> deleteAllStudies();


	@ApiOperation(value = "", notes = "Saves or updates a study", response = Void.class, tags = {})
	@ApiResponses(value = { @ApiResponse(code = 201, message = "study created", response = Void.class),
			@ApiResponse(code = 204, message = "study updated", response = Void.class),
			@ApiResponse(code = 200, message = "unexpected error", response = Void.class) })
	@RequestMapping(value = "/study", produces = { "application/json" }, method = RequestMethod.PUT)
	ResponseEntity<Void> saveStudy(
			@ApiParam(value = "id of the study", required = true) @RequestParam(value = "id", required = true) Long id,
			@ApiParam(value = "name of the study", required = true) @RequestParam(value = "name", required = true) String name);


	@ApiOperation(value = "", notes = "Updates the study list", response = Void.class, tags = {})
	@ApiResponses(value = { @ApiResponse(code = 204, message = "studies updated", response = Void.class),
			@ApiResponse(code = 200, message = "unexpected error", response = Void.class) })
	@RequestMapping(value = "/study/all", produces = { "application/json" }, consumes = {
			"application/json" }, method = RequestMethod.PUT)
	ResponseEntity<Void> updateStudies(

			@ApiParam(value = "the studies to save", required = true) @RequestBody Studies studies

	);

}
