/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.searchisko.api.rest.exception;

/**
 * Exception to further extend {@link IllegalArgumentException} to provide information about invalid fields.
 */
public class BadFieldException extends IllegalArgumentException {
	private String fieldName;

	private String description;

	public BadFieldException(String fieldName, Throwable cause) {
		super(fieldName, cause);
		this.fieldName = fieldName;
	}

	/**
	 * @param fieldName name of bad field
	 */
	public BadFieldException(String fieldName) {
		super(fieldName);
		this.fieldName = fieldName;
	}

	/**
	 * @param fieldName name of bad field
	 * @param description optional detailed description why is field bad.
	 */
	public BadFieldException(String fieldName, String description) {
		super("fieldName=" + fieldName + ", description=" + description);
		this.fieldName = fieldName;
		this.description = description;
	}

	/**
	 * @return name of bad field
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * @return optional detailerd description why is field bad
	 */
	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return "BadFieldException [fieldName=" + fieldName + ", description=" + description + "]";
	}

}
