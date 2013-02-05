/**
 *
 * (c) 2003-2012 MuleSoft, Inc. This software is protected under international
 * copyright law. All use of this software is subject to MuleSoft's Master
 * Subscription Agreement (or other Terms of Service) separately entered
 * into between you and MuleSoft. If such an agreement is not in
 * place, you may not use the software.
 */

package org.mule.module.hubspot.serialization;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.mule.module.hubspot.model.contact.ContactProperties;
import org.mule.module.hubspot.model.contact.ContactPropertiesLifecycleStage;
import org.mule.module.hubspot.model.contact.ContactPropertiesNumberOfEmployees;

public class ContactJacksonDeserializer extends
		JsonDeserializer<ContactProperties> {

	@Override
	public ContactProperties deserialize(JsonParser jp,
			DeserializationContext ctxt) throws IOException,
			JsonProcessingException {

		ContactProperties cp = new ContactProperties();

		PropertyDescriptor[] propertyDescriptor;
		try {
			propertyDescriptor = Introspector.getBeanInfo(
					ContactProperties.class).getPropertyDescriptors();
		} catch (IntrospectionException e) {
			throw new IOException(e);
		}

		String propertyName;
		String propertyValue;

		// Main object "properties": { ... }
		if (jp.getCurrentToken().equals(JsonToken.START_OBJECT)) {
			while (!jp.nextToken().equals(JsonToken.END_OBJECT)) {
				propertyName = null;
				propertyValue = null;
				
				// Property name
				if (jp.getCurrentToken().equals(JsonToken.FIELD_NAME)) {
					propertyName = jp.getCurrentName();
					jp.nextToken();
					
					// Object inside property "properties" : { "PROPERTY_NAME" : {  ... } } 
					if (jp.getCurrentToken().equals(JsonToken.START_OBJECT)) {
						while (!jp.nextToken().equals(JsonToken.END_OBJECT)) {
							
							// Scan the field name "properties" : { "PROPERTY_NAME" : { "FIELDNAME" } }
							if (jp.getCurrentToken().equals(JsonToken.FIELD_NAME)) {
								if (jp.getCurrentName().equalsIgnoreCase("value")) {
									JsonToken jtoken = jp.nextValue();									
									propertyValue = jp.getText();
									
									// Filter the value (it must match some of the allowed values)
									if (jtoken.equals(JsonToken.VALUE_STRING) || jtoken.equals(JsonToken.VALUE_FALSE) || jtoken.equals(JsonToken.VALUE_TRUE) 
											|| jtoken.equals(JsonToken.VALUE_NUMBER_INT) || jtoken.equals(JsonToken.VALUE_NUMBER_FLOAT)) {
								
										propertyValue = jp.getText();
										
										PropertyDescriptor pd = findPropertyDescriptorByName(propertyDescriptor, propertyName);
										if (pd != null && pd.getWriteMethod() != null && !"class".equals(pd.getName())) {
											Class<?> classType = pd.getPropertyType();
											if (classType.equals(Long.class)) {
												try {
													pd.getWriteMethod().invoke(cp, Long.parseLong(propertyValue));
												} catch (NumberFormatException e) {
													throw new IOException(e);
												} catch (Exception e) {
													throw new IOException(e);
												}
											} else if (classType.equals(String.class)) {
												try {
													pd.getWriteMethod().invoke(cp, propertyValue);
												} catch (NumberFormatException e) {
													throw new IOException(e);
												} catch (Exception e) {
													throw new IOException(e);
												}
											} else if (classType.equals(ContactPropertiesLifecycleStage.class)) {
												try {
													pd.getWriteMethod().invoke(cp, ContactPropertiesLifecycleStage.getFromString(propertyValue));
												} catch (NumberFormatException e) {
													throw new IOException(e);
												} catch (Exception e) {
													throw new IOException(e);
												}
											} else if (classType.equals(ContactPropertiesNumberOfEmployees.class)) {
												try {
													pd.getWriteMethod().invoke(cp, ContactPropertiesNumberOfEmployees.getFromString(propertyValue));
												} catch (NumberFormatException e) {
													throw new IOException(e);
												} catch (Exception e) {
													throw new IOException(e);
												}
											} 
										}
									}
								} else if (jp.getCurrentName().equalsIgnoreCase("versions")) {
									// "properties": { "PROPERTY_NAME" : { "versions" : [ ... ] } }
									if (jp.nextToken().equals(JsonToken.START_ARRAY)) {
										while (!jp.nextToken().equals(JsonToken.END_ARRAY)) {
											
											// "properties": { "PROPERTY_NAME" : { "versions" : [ { ... } ] } }
											if (jp.getCurrentToken().equals(JsonToken.START_OBJECT)) {
												while (!jp.nextToken().equals(JsonToken.END_OBJECT)) {
													
												}
											}
										}
									}
								}
							}
						}
					}
				}	
			}
		}

		return cp;
	}
	
	private PropertyDescriptor findPropertyDescriptorByName(PropertyDescriptor[] props, String name) {
		for (PropertyDescriptor pd : props) {
			if (pd.getName().equalsIgnoreCase(name)) {
				return pd;
			}
		}
		
		return null;
	}

}