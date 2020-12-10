package io.digisic.bank.controller.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import io.digisic.bank.config.DTPConfig;
import io.digisic.bank.exception.RestServiceUnavailableException;
import io.digisic.bank.model.UserProfile;
import io.digisic.bank.model.security.Users;
import io.digisic.bank.service.DTPService;
import io.digisic.bank.service.UserService;
import io.digisic.bank.util.Constants;
import io.digisic.bank.util.DTPUtil;
import io.digisic.bank.util.Messages;
import io.digisic.bank.util.Patterns;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(Constants.URI_USER)
public class WebUserController extends WebCommonController {
	
	// Class Logger
	private static final Logger LOG = LoggerFactory.getLogger(WebUserController.class);
		  
	@Autowired
	private UserService userService;

	@Autowired
	DTPService dtpService;

	@Autowired
	DTPConfig dtpConfig;

	@GetMapping(Constants.URI_USR_PASSWORD)
	public String password(Principal principal, Model model) {
    
		// Set Display Defaults
		setDisplayDefaults(principal, model);
		
		// Add format patterns
		model.addAttribute(MODEL_ATT_PATTERN_PASSWORD, Patterns.USER_PASSWORD);
		model.addAttribute(MODEL_ATT_PATTERN_PASSWORD_MSG, Messages.USER_PASSWORD_FORMAT);
		    
		return Constants.VIEW_USR_PASSWORD;
	}
	
	@PostMapping(Constants.URI_USR_PASSWORD)
	public String password(Principal principal, Model model,
						   @ModelAttribute(MODEL_ATT_NEW_PASS) String newPassword,
						   @ModelAttribute(MODEL_ATT_CUR_PASS) String oldPassword) {
    
		// Set Display Defaults
		setDisplayDefaults(principal, model);
				
		Users user = userService.findByUsername(principal.getName());
		
		LOG.debug("Change Password: Validate Password Entries.");
		
		// Validate Password entries
		if (userService.passwordMatches(user, oldPassword)) {
			
			LOG.debug("Change Password: Current Password is correct.");
			
			if (newPassword.equals(oldPassword)) {
				
				// new password matches current password, throw error
				
				LOG.debug("Change Password: New Password is the same as the current password.");
				
				model.addAttribute(MODEL_ATT_ERROR_MSG, Messages.USER_PASSWORD_SAME);
				
			} else {
				
				// change password
				LOG.debug("Change Password: New Password is a actually a new password. Update Password.");
				
				userService.changePassword(user, newPassword);
				model.addAttribute(MODEL_ATT_SUCCESS_MSG, Messages.USER_PASSWORD_UPDATED);
				
			}
			
		} else {
			
			LOG.debug("Change Password: Current Password is NOT correct.");
			
			// old password not correct, throw error
			model.addAttribute(MODEL_ATT_ERROR_MSG, Messages.USER_PASSWORD_NO_MATCH);
			
		}
		
		// Add format patterns
		model.addAttribute(MODEL_ATT_PATTERN_PASSWORD, Patterns.USER_PASSWORD);
		model.addAttribute(MODEL_ATT_PATTERN_PASSWORD_MSG, Messages.USER_PASSWORD_FORMAT);
		    
		return Constants.VIEW_USR_PASSWORD;
	}
	
	@GetMapping(Constants.URI_USR_PROFILE)
	public String profile(Principal principal, Model model) {
    
		
		// Set Display Defaults
		setDisplayDefaults(principal, model);
				
		Users user = userService.findByUsername(principal.getName());
		
		model.addAttribute(MODEL_ATT_USER_PROFILE, user.getUserProfile());
		
		// Add format patterns
	    model.addAttribute(MODEL_ATT_PATTERN_PHONE, Patterns.USER_PHONE_REQ);
	    model.addAttribute(MODEL_ATT_PATTERN_PHONE_MSG, Messages.USER_PHONE_GENERIC_FORMAT);
		    
		return Constants.VIEW_USR_PROFILE;
	}
	
	@PostMapping(value = Constants.URI_USR_PROFILE, params="action=update")
	public String profile(Principal principal, Model model,
			 			  @ModelAttribute(MODEL_ATT_USER_PROFILE) UserProfile updateProfile) {
    
		// Set Display Defaults
		setDisplayDefaults(principal, model);
				
		Users user = userService.findByUsername(principal.getName());
		
		user = userService.updateUserProfile(user, updateProfile);
		
		// Add format patterns
	    model.addAttribute(MODEL_ATT_PATTERN_PHONE, Patterns.USER_PHONE_REQ);
	    model.addAttribute(MODEL_ATT_PATTERN_PHONE_MSG, Messages.USER_PHONE_GENERIC_FORMAT);
	    
		model.addAttribute(MODEL_ATT_USER_PROFILE, user.getUserProfile());
		model.addAttribute(MODEL_ATT_SUCCESS_MSG, Messages.USER_PROFILE_UPDATED);
		    
		return Constants.VIEW_USR_PROFILE;
	}

	@PostMapping(value = Constants.URI_USR_PROFILE, params="action=validate")
	public RedirectView validate(Principal principal, Model model,
								 @ModelAttribute(MODEL_ATT_USER_PROFILE) UserProfile updateProfile) {

		// Set Display Defaults
		setDisplayDefaults(principal, model);
		String redirectUrl = null;
		try {

			Users user = userService.findByUsername(principal.getName());

			String jsonClaims = new JSONObject()
				.put("purpose", "In order to allow you to make money transfers, we need to validate some details about you")
				.put("id_token", new JSONObject()
					.put("assertion_claims", new JSONObject()
						.put("age", new JSONObject()
							.put("purpose", "We would like to check you are older than 21")
							.put("essential", false)
							.put("ial", 1)
							.put("assertion", new JSONObject()
								.put("gte", 21)))
						.put("address", new JSONObject()
							.put("purpose", "We want to check some of your address details")
							.put("essential", true)
							.put("ial", 1)
							.put("assertion", new JSONObject()
								.put("props", new JSONObject()
									.put("country", new JSONObject()
										.put("eq", user.getUserProfile().getCountry()))
									.put("postal_code", new JSONObject()
											.put("eq", user.getUserProfile().getPostalCode()))

								)))
						/*.put("total_balance", new JSONObject()
							.put("purpose", "We would like to ensure you have a healthy balance")
							.put("essential", false)
							.put("ial", 1)
							.put("assertion", new JSONObject()
								.put("props", new JSONObject()
									.put("amount", new JSONObject()
										.put("gte", 500))
									.put("currency", new JSONObject()
										.put("eq", "GBP")))))*/
					)
					.put("email", new JSONObject()
							.put("purpose", "We will need to validate your email match our records")
							.put("essential", true))
					.put("phone_number", new JSONObject()
							.put("purpose", "We will need your real phone number to get in touch in case any suspicious activity on your account")
							.put("essential", true))
					.put("given_name", new JSONObject()
							.put("purpose", "We will need your real first name to confirm it's really you")
							.put("essential", true))
					.put("family_name", new JSONObject()
							.put("purpose", "We will need your real last name to confirm it's really you")
							.put("essential", true))
				)
				.toString();

			System.out.println("CLAIMS: " + jsonClaims);

			redirectUrl = dtpService.validateUser(DTPConfig.redirectURI, jsonClaims);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return new RedirectView(redirectUrl);
	}

	@GetMapping(Constants.URI_USR_PROFILE_AUTHENTICATE)
	public String authenticateUser (Principal principal, Model model, @RequestParam String code, @RequestParam String state){

		try {

			System.out.println("Authenticating user code on WebUserController... " + code);

			Users user = userService.findByUsername(principal.getName());

			PrivateKeyJWT privateKeyJWT = new PrivateKeyJWT(DTPConfig.clientID, new URI(dtpConfig.getIssuer()), JWSAlgorithm.RS256,
					DTPConfig.RSAKey.toRSAPrivateKey(), dtpConfig.getKid(), null);

			AuthenticationRequest request = new AuthenticationRequest.Builder(new URI(dtpConfig.getIssuer()), DTPConfig.clientID)
					.customParameter("grant_type", GrantType.AUTHORIZATION_CODE.getValue())
					.customParameter("code", code)
					.customParameter("client_assertion_type", PrivateKeyJWT.CLIENT_ASSERTION_TYPE)
					.customParameter("client_assertion", privateKeyJWT.getClientAssertion().serialize())
					.customParameter("redirect_uri", DTPConfig.redirectURI)
					.endpointURI(new URI(dtpConfig.getTokenUrl()))
					.build();

			HTTPRequest requestAuthorize = request.toHTTPRequest(HTTPRequest.Method.POST);
			requestAuthorize.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
			HTTPResponse httpResponse = requestAuthorize.send();
			System.out.println("RESPONSE TOKEN: " +  httpResponse.getStatusMessage());
			System.out.println("RESPONSE TOKEN CONTENT:   " +  httpResponse.getContent());

			ObjectMapper mapper = new ObjectMapper();
			JsonNode responseData = mapper.readTree(httpResponse.getContent());

			JWSObject jwsObject = JWSObject.parse(responseData.get("id_token").asText());

			Payload payload = jwsObject.getPayload();

			responseData = mapper.readTree(payload.toString());

			System.out.println("CLAIMS RESULT: " + responseData);

			//Validate received data vs User profile data
			UserProfile up = user.getUserProfile();

			DTPUtil dtpUtil = new DTPUtil(responseData);
			List<String> failedClaims = new ArrayList<>();

			if (responseData.get("email") == null || !up.getEmailAddress().equals(responseData.get("email").asText()))
				failedClaims.add("Email");

			if (responseData.get("phone_number") == null || !up.getHomePhone().equals(responseData.get("phone_number").asText()))
				failedClaims.add("Home phone");

			if (responseData.get("given_name") == null || !up.getFirstName().equals(responseData.get("given_name").asText()))
				failedClaims.add("First name");

			if (responseData.get("family_name") == null || !up.getLastName().equals(responseData.get("family_name").asText()))
				failedClaims.add("Last name");

			if (!dtpUtil.getAssertion("address"))
				failedClaims.add("Address");

			if (!dtpUtil.getAssertion("age"))
				System.out.println("WARNING: He is under 21 years old");
				//failedClaims.add("Postal code");

			if (failedClaims.isEmpty()) {
				user = userService.verifyProfile(user);
				model.addAttribute(MODEL_ATT_SUCCESS_MSG, Messages.USER_PROFILE_VERIFIED_SUCCESS);
			} else {
				model.addAttribute(MODEL_ATT_ERROR_MSG, "There were inconsistencies in the profile verification: " + String.join(", ", failedClaims));
			}

			// Set Display Defaults
			setDisplayDefaults(principal, model);

			model.addAttribute(MODEL_ATT_USER_PROFILE, user.getUserProfile());

			// Add format patterns
			model.addAttribute(MODEL_ATT_PATTERN_PHONE, Patterns.USER_PHONE_REQ);
			model.addAttribute(MODEL_ATT_PATTERN_PHONE_MSG, Messages.USER_PHONE_GENERIC_FORMAT);

		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RestServiceUnavailableException("Problem occurred!");
		}

		return Constants.VIEW_USR_PROFILE;

	}
}
