/**
 * (c) 2003-2012 MuleSoft, Inc. This software is protected under international
 * copyright law. All use of this software is subject to MuleSoft's Master
 * Subscription Agreement (or other Terms of Service) separately entered
 * into between you and MuleSoft. If such an agreement is not in
 * place, you may not use the software.
 **/

/**
 * This file was automatically generated by the Mule Development Kit
 */
package org.mule.module.hubspot;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.annotations.param.OutboundHeaders;
import org.mule.module.hubspot.client.HubSpotClient;
import org.mule.module.hubspot.client.impl.HubSpotClientImpl;
import org.mule.module.hubspot.credential.HubSpotCredentialsManager;
import org.mule.module.hubspot.exception.HubSpotConnectorAccessTokenExpiredException;
import org.mule.module.hubspot.exception.HubSpotConnectorException;
import org.mule.module.hubspot.exception.HubSpotConnectorNoAccessTokenException;
import org.mule.module.hubspot.model.OAuthCredentials;
import org.mule.module.hubspot.model.contact.Contact;
import org.mule.module.hubspot.model.contact.ContactDeleted;
import org.mule.module.hubspot.model.contact.ContactList;
import org.mule.module.hubspot.model.contact.ContactProperties;
import org.mule.module.hubspot.model.contact.ContactQuery;
import org.mule.module.hubspot.model.contact.ContactStatistics;
import org.mule.module.hubspot.model.contactproperty.CustomContactProperty;
import org.mule.module.hubspot.model.list.HubSpotList;
import org.mule.module.hubspot.model.list.HubSpotListLists;
import org.springframework.core.annotation.Order;

/**
 * HubSpot all-in-one marketing software helps more than 8,000 companies in 56 countries attract leads and convert them into customers. 
 * A pioneer in inbound marketing, HubSpot aims to help its customers make marketing that people actually love.
 * <p>
 * The connector is using the version "v1" of the HubSpot API.
 * <p>
 * The documentation of the API can be found in this <a href="http://developers.hubspot.com/docs">link</a>
 * <p>
 * The main flow of the connector is "authentication" ---> HubSpot Login Page ----> "authenticationResponse" ----> Any other process of the connector
 *
 * @author MuleSoft, Inc.
 */
@Connector(name="hubspot", schemaVersion="2.2.2", friendlyName="HubSpot")
public class HubSpotConnector
{
	static final private String HUB_SPOT_URL_API 		= "http://hubapi.com";
	static final private String HUB_SPOT_URL_AUTH		= "https://app.hubspot.com/auth/authenticate";
	static final private String API_VERSION				= "v1";
	
	/**
	 * Your Client ID (OAuth Client ID), which identifies who you are. You can access the client_id in your app's developer dashboard under the Summary section.
	 */
	@Configurable
	@Order(1)
	private String clientId;
	
	/**
	 * The HubSpot portal ID of the customer that you're re-directing. You will need to get the portal ID from the customer who you're making the request for.
	 * <p>
	 * In order to find the Hub ID follow this link: <a href="http://help.hubspot.com/articles/How_To_Doc/How-to-find-your-hub-id">http://help.hubspot.com/articles/How_To_Doc/How-to-find-your-hub-id</a>
	 */
	@Configurable
	@Order(2)
	private String hubId;
	
	/**
	 * The scopes (or permissions) you want. These should match your application settings in the Marketplace. Separate more than one scope with "+".	 * 
	 * <p>
	 * <b>Important:</b> the scope offline provides the ability to refresh the token automatically once this has expired. If you do not specify this scope, once the token
	 * expires the call to a process will throw a HubSpotConnectorAccessTokenExpiredException
	 * <p>
	 * <b>Note:</b> the scope required in the authentication must be supported by the application. This can be checked in the Application Settings, under Scopes
	 * <p>
	 * For a complete list of the available scopes check this link: <a href="http://developers.hubspot.com/auth/oauth_scopes">http://developers.hubspot.com/auth/oauth_scopes</a>
	 */
	@Configurable
	@Order(3)
	private String scope;
	
	/**
	 * The callbackUrl is the endpoint that is registered in the iApp to handle the response of the 
	 * authorization call. This endpoint also has to direct to the handleAuthentication process of the connector
	 */
	@Configurable
	@Order(4)
	private String callbackUrl;
	
	private HubSpotCredentialsManager credentialsManager;
	
	private HubSpotClient client;
	
	@PostConstruct
	public void initialize() {
		credentialsManager = new HubSpotCredentialsManager();
		client = new HubSpotClientImpl(HUB_SPOT_URL_API, HUB_SPOT_URL_AUTH, API_VERSION, clientId, hubId, scope, callbackUrl);
	}

	/**
	 * This process generates the URL required to authenticate against the service.
	 * <p>
	 * <b>Important:</b> in order for the full authentication to work, the callbackUrl in the configuration must be
	 * pointing to another flow that has the authenticateResponse process to handle the reception of the token
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:authenticate}
	 * 
	 * @param userId This user identifier it is the one that will we used from now on to the successive calls to the process of this connector for this user
	 * @param headers This are added implicitly by Studio. The headers of the HTTP inbound, so it can establish a redirect code (302)
	 * @return The URL where the user will be redirected
	 * @throws HubSpotConnectorException If occur some error trying to generate the URL or the userId is empty it will throw this exception.
	 */
	@Processor
	public String authenticate(String userId, @OutboundHeaders Map<String, Object> headers) throws HubSpotConnectorException {
		return client.authenticate(userId, headers);
	}
	
	/**
	 * This process is the one that handles the response of the authentication process. It should be inside an HTTP inbound which
	 * url must be the same that the one pointed by the callbackUrl in the configuration in order to get
	 * the access_token provided by the service.
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:authenticate-response}
	 * 
	 * @param inputRequest The input parameters that came with the response to the authenticate process
	 * @return The UserID that you provided in the call to the authenticate process and that is the one that the user is going to provide in order than the connector use their credentials 
	 * @throws HubSpotConnectorException If any one of the required parameters is empty it will throw this exception.
	 * @throws HubSpotConnectorNoAccessTokenException If there is not an access_token in the response it will throw this exception.
	 */
	@Processor
	public String authenticateResponse(String inputRequest) throws HubSpotConnectorException, HubSpotConnectorNoAccessTokenException {
		
		OAuthCredentials credentials = client.authenticateResponse(inputRequest);
		credentialsManager.setCredentias(credentials);
		
		return credentials.getUserId();
	}
	
	/**
	 * Check if the User has an Access Token. This indicate that this User can start calling the process of the connector without any problems
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:has-user-access-token}
	 * 
	 * @param userId The UserID of the user in the HubSpot service that was obtained from the {@link authenticateResponse} process
	 * @return A boolean that indicates if the user has an access token. Id does not check if the token is or not expired
	 */
	@Processor
	public boolean hasUserAccessToken(String userId) {
		return credentialsManager.hasUserAccessToken(userId);
	}	
	
	//
	/**
	 * For a given portal, return all contacts that have been created in the portal.
	 * A paginated list of contacts will be returned to you, with a maximum of 100 contacts per page.
	 * <p>
	 * API Link: <a href="http://developers.hubspot.com/docs/methods/contacts/get_contacts">http://developers.hubspot.com/docs/methods/contacts/get_contacts</a>
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:get-all-contacts}
	 * 
	 * @param userId The UserID of the user in the HubSpot service that was obtained from the {@link authenticateResponse} process
	 * @param count This parameter lets you specify the amount of contacts to return in your API call. The default for this parameter (if it isn't specified) is 20 contacts. The maximum amount of contacts you can have returned to you via this parameter is 100.
	 * @param contactOffset This parameter will offset the contacts returned to you, based on the unique ID of the contacts in a given portal. Contact unique IDs are assigned by the order that they are created in the system. This means for instance, if you specify a vidOffset offset of 5, and you have 20 contacts in the portal you're working in, the contacts with IDs 6-20 will be returned to you.
	 * @return A {@link ContactList} containing all the contacts
	 * @throws HubSpotConnectorException If the required parameters were not specified or occurs another type of error this exception will be thrown
	 * @throws HubSpotConnectorNoAccessTokenException If the user does not have an Access Token this exception will be thrown
	 * @throws HubSpotConnectorAccessTokenExpiredException If the user has his token already expired this exception will be thrown
	 */
	@Processor
	public ContactList getAllContacts(String userId, @Optional @Default("") String count, @Optional @Default("") String contactOffset) 
			throws HubSpotConnectorException, HubSpotConnectorNoAccessTokenException, HubSpotConnectorAccessTokenExpiredException {
		
		return client.getAllContacts(credentialsManager.getCredentialsAccessToken(userId), userId, count, contactOffset);
	}
	
	
	/**
	 * For a given portal, return all contacts that have been recently updated or created.
	 * A paginated list of contacts will be returned to you, with a maximum of 100 contacts per page, as specified by the "count" parameter.
	 * <p>
	 * API link: <a href="http://developers.hubspot.com/docs/methods/contacts/get_recently_updated_contacts">http://developers.hubspot.com/docs/methods/contacts/get_recently_updated_contacts</a>
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:get-recent-contacts}
	 * 
	 * @param userId The UserID of the user in the HubSpot service that was obtained from the {@link authenticateResponse} process
	 * @param count This parameter lets you specify the amount of contacts to return in your API call. The default for this parameter (if it isn't specified) is 20 contacts. The maximum amount of contacts you can have returned to you via this parameter is 100.
	 * @param timeOffset Used in conjunction with the vidOffset paramter to page through the recent contacts. Every call to this endpoint will return a time-offset value. This value is used in the timeOffset parameter of the next call to get the next page of contacts.
	 * @param contactOffset Used in conjunction with the timeOffset paramter to page through the recent contacts. Every call to this endpoint will return a vid-offset value. This value is used in the vidOffset parameter of the next call to get the next page of contacts.
	 * @return A {@link ContactList} containing all the contacts
	 * @throws HubSpotConnectorException If the required parameters were not specified or occurs another type of error this exception will be thrown
	 * @throws HubSpotConnectorNoAccessTokenException If the user does not have an Access Token this exception will be thrown
	 * @throws HubSpotConnectorAccessTokenExpiredException If the user has his token already expired this exception will be thrown
	 */
	@Processor
	public ContactList getRecentContacts(String userId, @Optional @Default("") String count, @Optional @Default("") String timeOffset, @Optional @Default("") String contactOffset)
			throws HubSpotConnectorException, HubSpotConnectorNoAccessTokenException, HubSpotConnectorAccessTokenExpiredException {
		
		return client.getRecentContacts(credentialsManager.getCredentialsAccessToken(userId), userId, count, timeOffset, contactOffset);
	}
	
	
	/**
	 * For a given portal, return information about a single contact by its ID. The contact's unique ID's is stored in a field called 'vid' which stands for 'visitor ID'.
	 * This method will also return you much of the HubSpot lead "intelligence" that you may be accustomed to getting from the leads API, as properties in this new API. 
	 * More of this intelligence will be available as time passes, but this call is where you can expect to find it.
	 * <p>
	 * API link: <a href="http://developers.hubspot.com/docs/methods/contacts/get_contact">http://developers.hubspot.com/docs/methods/contacts/get_contact</a>
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:get-contact-by-id}
	 * 
	 * @param userId The UserID of the user in the HubSpot service that was obtained from the {@link authenticateResponse} process
	 * @param contactId Unique identifier for a particular contact. In HubSpot's contact system, contact ID's are called "vid".
	 * @return The {@link Contact} representation
	 * @throws HubSpotConnectorException If the required parameters were not specified or occurs another type of error this exception will be thrown
	 * @throws HubSpotConnectorNoAccessTokenException If the user does not have an Access Token this exception will be thrown
	 * @throws HubSpotConnectorAccessTokenExpiredException If the user has his token already expired this exception will be thrown
	 */
	@Processor
	public Contact getContactById(String userId, String contactId)
			throws HubSpotConnectorException, HubSpotConnectorNoAccessTokenException, HubSpotConnectorAccessTokenExpiredException {
		
		return client.getContactById(credentialsManager.getCredentialsAccessToken(userId), userId, contactId);
	}
	
	/**
	 * For a given portal, return information about a single contact by its email address.
	 * <p>
	 * API link: <a href="http://developers.hubspot.com/docs/methods/contacts/get_contact_by_email">http://developers.hubspot.com/docs/methods/contacts/get_contact_by_email</a>
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:get-contact-by-email}
	 * 
	 * @param userId The UserID of the user in the HubSpot service that was obtained from the {@link authenticateResponse} process
	 * @param contactEmail The email address for the contact that you're searching for.
	 * @return The {@link Contact} representation
	 * @throws HubSpotConnectorException If the required parameters were not specified or occurs another type of error this exception will be thrown
	 * @throws HubSpotConnectorNoAccessTokenException If the user does not have an Access Token this exception will be thrown
	 * @throws HubSpotConnectorAccessTokenExpiredException If the user has his token already expired this exception will be thrown
	 */
	@Processor
	public Contact getContactByEmail(String userId, String contactEmail)
			throws HubSpotConnectorException, HubSpotConnectorNoAccessTokenException, HubSpotConnectorAccessTokenExpiredException {
				
		return client.getContactByEmail(credentialsManager.getCredentialsAccessToken(userId), userId, contactEmail);
	}
	
	/**
	 * For a given portal, return information about a single contact by its User Token (hubspotutk)
	 * <p>
	 * API link: <a href="http://developers.hubspot.com/docs/methods/contacts/get_contact_by_utk">http://developers.hubspot.com/docs/methods/contacts/get_contact_by_utk</a>
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:get-contact-by-user-token}
	 * 
	 * @param userId The UserID of the user in the HubSpot service that was obtained from the {@link authenticateResponse} process
	 * @param contactUserToken The user token (HubSpot cookie) for the contact that you're searching for.
	 * @return The {@link Contact} representation
	 * @throws HubSpotConnectorException If the required parameters were not specified or occurs another type of error this exception will be thrown
	 * @throws HubSpotConnectorNoAccessTokenException If the user does not have an Access Token this exception will be thrown
	 * @throws HubSpotConnectorAccessTokenExpiredException If the user has his token already expired this exception will be thrown
	 */
	@Processor
	public Contact getContactByUserToken(String userId, String contactUserToken)
			throws HubSpotConnectorException, HubSpotConnectorNoAccessTokenException, HubSpotConnectorAccessTokenExpiredException {
		
		return client.getContactByUserToken(credentialsManager.getCredentialsAccessToken(userId), userId, contactUserToken);
	}
	
	/**
	 * For a given portal, return contacts and some data associated with those contacts by the contact's email address or name.
	 * Please note that you should expect this method to only return a small subset of data about the contact. One piece of data 
	 * that the method will return is the contact ID (vid) that you can then use to look up much more data about that particular contact by its ID.
	 * <p>
	 * API link: <a href="http://developers.hubspot.com/docs/methods/contacts/search_contacts">http://developers.hubspot.com/docs/methods/contacts/search_contacts</a>
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:get-contacts-by-query}
	 * 
	 * @param userId The UserID of the user in the HubSpot service that was obtained from the {@link authenticateResponse} process
	 * @param query The search term for what you're searching for. You can use all of a word or just parts of a word as well. For example, if you we're searching for contacts with "hubspot" in their name or email, searching for "hub" would also return contacts with "hubspot" in their email address.
	 * @param count This parameter lets you specify the amount of contacts to return in your API call. The default for this parameter (if it isn't specified) is 20 contacts. The maximum amount of contacts you can have returned to you via this parameter is 100.
	 * @return A {@link ContactQuery} with the contacts
	 * @throws HubSpotConnectorException If the required parameters were not specified or occurs another type of error this exception will be thrown
	 * @throws HubSpotConnectorNoAccessTokenException If the user does not have an Access Token this exception will be thrown
	 * @throws HubSpotConnectorAccessTokenExpiredException If the user has his token already expired this exception will be thrown
	 */
	@Processor
	public ContactQuery getContactsByQuery(String userId, String query, @Optional @Default("") String count)
			throws HubSpotConnectorException, HubSpotConnectorNoAccessTokenException, HubSpotConnectorAccessTokenExpiredException {
		
		return client.getContactsByQuery(credentialsManager.getCredentialsAccessToken(userId), userId, query, count);
	}
		
	/**
	 * Archive an existing contact from a particular HubSpot portal. 
	 * Archiving will not hard delete a contact from a portal, but will remove that contact from the HubSpot user interface.
	 * <p>
	 * API link: <a href="http://developers.hubspot.com/docs/methods/contacts/delete_contact">http://developers.hubspot.com/docs/methods/contacts/delete_contact</a>
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:delete-contact}
	 * 
	 * @param userId The UserID of the user in the HubSpot service that was obtained from the {@link authenticateResponse} process
	 * @param contactId You must pass the Contact's ID that you're archiving in the request URL.
	 * @return A {@link ContactDeleted} representing the data when the contact is deleted
	 * @throws HubSpotConnectorException If the required parameters were not specified or occurs another type of error this exception will be thrown
	 * @throws HubSpotConnectorNoAccessTokenException If the user does not have an Access Token this exception will be thrown
	 * @throws HubSpotConnectorAccessTokenExpiredException If the user has his token already expired this exception will be thrown
	 */
	@Processor
	public ContactDeleted deleteContact(String userId, String contactId)
			throws HubSpotConnectorException, HubSpotConnectorNoAccessTokenException, HubSpotConnectorAccessTokenExpiredException {
		
		return client.deleteContact(credentialsManager.getCredentialsAccessToken(userId), userId, contactId);
		
	}
	
	/**
	 * Update an existing contact in HubSpot. This method lets you update one of many fields of a contact in HubSpot.
	 * <p>
	 * To update a contact, you should make an HTTP POST call to this endpoint with some JSON in the request payload. 
	 * This JSON should contain properties from the contact that you want to add to or update. See the sample JSON below for an example of this snippet of JSON.
	 * <p>
	 * If you are trying to close a contact into a customer via the API, you should be updating the 'lifecyclestage' property and setting the value of this property to 'customer'.
	 * <p>
	 * Remember, if a property doesn't yet exist, you can create a new custom property through the API by using the 'Create Property' method.
	 * <p>
	 * API link: <a href="http://developers.hubspot.com/docs/methods/contacts/update_contact">http://developers.hubspot.com/docs/methods/contacts/update_contact</a>
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:update-contact}
	 * 
	 * @param userId The UserID of the user in the HubSpot service that was obtained from the {@link authenticateResponse} process
	 * @param contactId You must pass the Contact's ID that you're updating in the request URL
	 * @param contactProperties The properties of the Contact that will have the one to be created
	 * @return The {@link ContactProperties} that was provided as input param
	 * @throws HubSpotConnectorException If the required parameters were not specified or occurs another type of error this exception will be thrown
	 * @throws HubSpotConnectorNoAccessTokenException If the user does not have an Access Token this exception will be thrown
	 * @throws HubSpotConnectorAccessTokenExpiredException If the user has his token already expired this exception will be thrown
	 */
	@Processor
	public ContactProperties updateContact(String userId, String contactId, ContactProperties contactProperties)
			throws HubSpotConnectorException, HubSpotConnectorNoAccessTokenException, HubSpotConnectorAccessTokenExpiredException {
		
		client.updateContact(credentialsManager.getCredentialsAccessToken(userId), userId, contactId, contactProperties);
		
		return contactProperties;
	}
	
	/**
	 * Create a new contact in HubSpot with a simple HTTP POST to the Contacts API.
	 * <p>
	 * API link: <a href="http://developers.hubspot.com/docs/methods/contacts/create_contact">http://developers.hubspot.com/docs/methods/contacts/create_contact</a>
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:create-contact}
	 * 
	 * @param userId The UserID of the user in the HubSpot service that was obtained from the {@link authenticateResponse} process
	 * @param contactProperties The properties that want to modify of an existing contact
	 * @return The {@link ContactProperties} that was provided as input param 
	 * @throws HubSpotConnectorException If the required parameters were not specified or occurs another type of error this exception will be thrown
	 * @throws HubSpotConnectorNoAccessTokenException If the user does not have an Access Token this exception will be thrown
	 * @throws HubSpotConnectorAccessTokenExpiredException If the user has his token already expired this exception will be thrown
	 */
	@Processor
	public ContactProperties createContact(String userId, ContactProperties contactProperties)
			throws HubSpotConnectorException, HubSpotConnectorNoAccessTokenException, HubSpotConnectorAccessTokenExpiredException {
		
		client.createContact(credentialsManager.getCredentialsAccessToken(userId), userId, contactProperties);
		
		return contactProperties;
	}
	
	/**
	 * For a given portal, return statistics about that portal's contacts.
	 * <p>
	 * API link: <a href="http://developers.hubspot.com/docs/methods/contacts/get_contact_statistics">http://developers.hubspot.com/docs/methods/contacts/get_contact_statistics</a>
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:get-contact-statistics}
	 * 
	 * @param userId The UserID of the user in the HubSpot service that was obtained from the {@link authenticateResponse} process
	 * @return A {@link ContactStatistics} representation of the response of statistics
	 * @throws HubSpotConnectorException If the required parameters were not specified or occurs another type of error this exception will be thrown
	 * @throws HubSpotConnectorNoAccessTokenException If the user does not have an Access Token this exception will be thrown
	 * @throws HubSpotConnectorAccessTokenExpiredException If the user has his token already expired this exception will be thrown
	 */
	@Processor
	public ContactStatistics getContactStatistics(String userId)
			throws HubSpotConnectorException, HubSpotConnectorNoAccessTokenException, HubSpotConnectorAccessTokenExpiredException {
		
		return client.getContactStatistics(credentialsManager.getCredentialsAccessToken(userId), userId);
	}
	
	/**
	 * For a given portal, return a set of contact lists that you specify with the count parameter.
	 * By default, we will only return up to 20 lists to you at a time.
	 * <p>
	 * API link: <a href="http://developers.hubspot.com/docs/methods/lists/get_lists">http://developers.hubspot.com/docs/methods/lists/get_lists</a>
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:get-contacts-lists}
	 * 
	 * @param userId The UserID of the user in the HubSpot service that was obtained from the {@link authenticateResponse} process
	 * @param count An integer that represents the number of lists that you want returned to your call. By default, this call will return 20 lists to you. If you want more or different list returned to you, you'll want to use the "offset" parameter.
	 * @param offset An integer that represents where to start your list pull from. For instance, if you want to return numbered lists: 50-60, your offset should be "50" and your count parameter (seen above) should be 10. You should also note that the returned JSON (seen below) includes a "has-more" field, which lets you know if there are more lists that you can pull. If "has-more" is true, you can use this offset parameter to pull lists that weren't in your initial call.
	 * @return A {@link HubSpotListLists} with the lists
	 * @throws HubSpotConnectorException If the required parameters were not specified or occurs another type of error this exception will be thrown
	 * @throws HubSpotConnectorNoAccessTokenException If the user does not have an Access Token this exception will be thrown
	 * @throws HubSpotConnectorAccessTokenExpiredException If the user has his token already expired this exception will be thrown
	 */
	@Processor
	public HubSpotListLists getContactsLists(String userId, @Optional @Default("") String count, @Optional @Default("") String offset) 
			throws HubSpotConnectorException, HubSpotConnectorNoAccessTokenException, HubSpotConnectorAccessTokenExpiredException {
		
		return client.getContactsLists(credentialsManager.getCredentialsAccessToken(userId), userId, count, offset);
	}
	
	/**
	 * For a given portal, return a contact list by its unique ID.
	 * <p>
	 * API link: <a href="http://developers.hubspot.com/docs/methods/lists/get_list">http://developers.hubspot.com/docs/methods/lists/get_list</a>
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:get-contact-list-by-id}
	 * 
	 * @param userId The UserID of the user in the HubSpot service that was obtained from the {@link authenticateResponse} process
	 * @param listId Unique identifier for the list that you're looking for.
	 * @return A {@link HubSpotList} with the list
	 * @throws HubSpotConnectorException If the required parameters were not specified or occurs another type of error this exception will be thrown
	 * @throws HubSpotConnectorNoAccessTokenException If the user does not have an Access Token this exception will be thrown
	 * @throws HubSpotConnectorAccessTokenExpiredException If the user has his token already expired this exception will be thrown
	 */
	@Processor
	public HubSpotList getContactListById(String userId, String listId)
			throws HubSpotConnectorException, HubSpotConnectorNoAccessTokenException, HubSpotConnectorAccessTokenExpiredException {
		
		return client.getContactListById(credentialsManager.getCredentialsAccessToken(userId), userId, listId);
	}
	
	/**
	 * For a given portal, return a set of dynamic contact lists that you specify with the count parameter.
	 * <p>
	 * Dynamic lists are lists that can only be edited by the contacts app - they are meant to update themselves 
	 * when new contacts are created or are updated, meaning that you can't manually add contacts to dynamic lists.
	 * <p>
	 * By default, we will only return 20 lists to you via this API call.
	 * <p>
	 * API link: <a href="http://developers.hubspot.com/docs/methods/lists/get_dynamic_lists">http://developers.hubspot.com/docs/methods/lists/get_dynamic_lists</a>
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:get-dynamic-contact-lists}
	 * 
	 * @param userId The UserID of the user in the HubSpot service that was obtained from the {@link authenticateResponse} process
	 * @param count An integer that represents the number of lists that you want returned to your call. By default, this call will return 20 lists to you. If you want more or different list returned to you, you'll want to use the "offset" parameter.
	 * @param offset An integer that represents where to start your list pull from. For instance, if you want to return numbered lists: 50-60, your offset should be "50" and your count parameter (seen above) should be 10. You should also note that the returned JSON (seen below) includes a "has-more" field, which lets you know if there are more lists that you can pull. If "has-more" is true, you can use this offset parameter to pull lists that weren't in your initial call.
	 * @return A {@link HubSpotListLists} with the lists
	 * @throws HubSpotConnectorException If the required parameters were not specified or occurs another type of error this exception will be thrown
	 * @throws HubSpotConnectorNoAccessTokenException If the user does not have an Access Token this exception will be thrown
	 * @throws HubSpotConnectorAccessTokenExpiredException If the user has his token already expired this exception will be thrown
	 */
	@Processor
	public HubSpotListLists getDynamicContactLists(String userId, @Optional @Default("") String count, @Optional @Default("") String offset)
			throws HubSpotConnectorException, HubSpotConnectorNoAccessTokenException, HubSpotConnectorAccessTokenExpiredException {
		
		return client.getDynamicContactLists(credentialsManager.getCredentialsAccessToken(userId), userId, count, offset);
	}
	
	/**
	 * 
	 * For a given portal and a given list, identified by its unique ID, return a list of contacts that are in that list.
	 * <p>
	 * API link: <a href="http://developers.hubspot.com/docs/methods/lists/get_list_contacts">http://developers.hubspot.com/docs/methods/lists/get_list_contacts</a>
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:get-contacts-in-a-list}
	 * 
	 * @param userId The UserID of the user in the HubSpot service that was obtained from the {@link authenticateResponse} process
	 * @param listId Unique identifier for the list that you're looking for.
	 * @param count This parameter lets you specify the amount of contacts to return in your API call. The default for this parameter (if it isn't specified) is 20 contacts. The maximum amount of contacts you can have returned to you via this parameter is 100.
	 * @param property If you include the "property" parameter, then the properties in the "contact" object in the returned data will only include the property or properties that you request.
	 * @param offset This parameter will offset the contacts returned to you, based on the unique ID of the contacts in a given portal. Contact unique IDs are assigned by the order that they are created in the system. This means for instance, if you specify a vidOffset offset of 5, and you have 20 contacts in the portal you're working in, the contacts with IDs 6-20 will be returned to you.
	 * @return A {@link ContactList} whit the contact list
	 * @throws HubSpotConnectorException If the required parameters were not specified or occurs another type of error this exception will be thrown
	 * @throws HubSpotConnectorNoAccessTokenException If the user does not have an Access Token this exception will be thrown
	 * @throws HubSpotConnectorAccessTokenExpiredException If the user has his token already expired this exception will be thrown
	 */
	@Processor
	public ContactList getContactsInAList(String userId, String listId, @Optional @Default("") String count, @Optional @Default("") String property, @Optional @Default("") String offset)
			throws HubSpotConnectorException, HubSpotConnectorNoAccessTokenException, HubSpotConnectorAccessTokenExpiredException {
		
		return client.getContactsInAList(credentialsManager.getCredentialsAccessToken(userId), userId, listId, count, property, offset);
	}
	
	/**
	 * Properties in HubSpot are fields that have been created. By default, there are many fields that come "out of the box" in a 
	 * HubSpot portal, but users can also create new, custom properties as they please.
	 * This method returns all of those properties to you.
	 * <p>
	 * API link: <a href="http://developers.hubspot.com/docs/methods/lists/get_list_contacts">http://developers.hubspot.com/docs/methods/lists/get_list_contacts</a>
	 * <p>
	 * {@sample.xml ../../../doc/HubSpot-connector.xml.sample hubspot:get-all-properties}
	 * 
	 * @param userId The UserID of the user in the HubSpot service that was obtained from the {@link authenticateResponse} process
	 * @return A List of {@link CustomContactProperty}
	 * @throws HubSpotConnectorException If the required parameters were not specified or occurs another type of error this exception will be thrown
	 * @throws HubSpotConnectorNoAccessTokenException If the user does not have an Access Token this exception will be thrown
	 * @throws HubSpotConnectorAccessTokenExpiredException If the user has his token already expired this exception will be thrown
	 */
	@Processor
	public List<CustomContactProperty> getAllProperties(String userId)
			throws HubSpotConnectorException, HubSpotConnectorNoAccessTokenException, HubSpotConnectorAccessTokenExpiredException {
		
		return client.getAllProperties(credentialsManager.getCredentialsAccessToken(userId), userId);
	}
	
	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getHubId() {
		return hubId;
	}

	public void setHubId(String hubId) {
		this.hubId = hubId;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getCallbackUrl() {
		return callbackUrl;
	}

	public void setCallbackUrl(String callbackUrl) {
		this.callbackUrl = callbackUrl;
	}
}
