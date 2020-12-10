package io.digisic.bank.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.client.ClientRegistrationErrorResponse;
import com.nimbusds.oauth2.sdk.client.ClientRegistrationResponse;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderConfigurationRequest;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.rp.*;
import io.digisic.bank.config.DTPConfig;
import io.digisic.bank.exception.RestServiceUnavailableException;
import io.digisic.bank.model.AtmLocation;
import io.digisic.bank.service.DTPService;
import io.digisic.bank.util.Messages;
import io.digisic.bank.util.Patterns;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import javax.validation.constraints.Pattern;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.UUID;

@RestController
public class DTPController {

    @Autowired
    DTPConfig dtpConfig;

    @Autowired
    DTPService dtpService;

    @GetMapping("/authenticate-user")
    public ResponseEntity<?> authenticateUser (@RequestParam String code, @RequestParam String state){

        try {

            System.out.println("Authenticating user2... " + code);

            PrivateKeyJWT privateKeyJWT = new PrivateKeyJWT(DTPConfig.clientID, new URI(dtpConfig.getIssuer()), JWSAlgorithm.RS256,
                    DTPConfig.RSAKey.toRSAPrivateKey(), dtpConfig.getKid(), null);

            AuthenticationRequest request = new AuthenticationRequest.Builder(new URI(dtpConfig.getIssuer()), DTPConfig.clientID)
                    .customParameter("grant_type", GrantType.AUTHORIZATION_CODE.getValue())
                    .customParameter("code", code)
                    .customParameter("client_assertion_type", PrivateKeyJWT.CLIENT_ASSERTION_TYPE)
                    //.customParameter("client_assertion", DTPConfig.privateKeyJWT.getClientAssertion().serialize())
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
            JsonNode actualObj = mapper.readTree(httpResponse.getContent());

            JWSObject jwsObject = JWSObject.parse(actualObj.get("id_token").asText());

            Payload payload = jwsObject.getPayload();

            actualObj = mapper.readTree(payload.toString());
            System.out.println("Email:  " + actualObj.get("email"));
            System.out.println("Phone:  " + actualObj.get("phone_number"));

            System.out.println("totalBalance: " + actualObj.get("assertion_claims").get("total_balance").get("result"));
            System.out.println("address: " + actualObj.get("assertion_claims").get("address").get("result"));
            System.out.println("age: " + actualObj.get("assertion_claims").get("age").get("result"));

            return ResponseEntity.ok(httpResponse.getContent());
        }
        catch (Exception ex) {
            ex.printStackTrace();
            throw new RestServiceUnavailableException("Problem occurred!");

        }

    }

    @GetMapping("/validate-user")
    public RedirectView validateUser () throws Exception {
        String redirectURI = DTPConfig.redirectURI;
        String issuer = dtpConfig.getIssuer();
        String kid = dtpConfig.getKid();

        RSAKey rsaKeyjwk = DTPConfig.RSAKey;

        RSAKey rsaPublicJWK = rsaKeyjwk.toPublicJWK();

        //SEND THE AUTHENTICATION REQUEST

        // The client ID provisioned by the OpenID provider when
        // the client was registered
        ClientID clientID = DTPConfig.clientID;

        // The client callback URL
        URI callback = new URI(redirectURI);

        // Generate random state string for pairing the response to the request
        State state = new State("myState");
        // Generate nonce
        Nonce nonce = new Nonce();
        // Specify scope
        Scope scope = Scope.parse("openid");

        // Prepare JWT with claims set
        JSONObject claims = (JSONObject) JSONValue.parse("{\"purpose\":\"In order to put you in contact with the employeer, we need to validate some details\",\"id_token\":{\"assertion_claims\":{\"age\":{\"purpose\":\"We want to check that you are older than 18\",\"essential\":true,\"ial\":1,\"assertion\":{\"gte\":18}},\"address\":{\"purpose\":\"We want to check you are UK resident\",\"essential\":true,\"ial\":1,\"assertion\":{\"props\":{\"country\":{\"eq\":\"United Kingdom\"}}}},\"total_balance\":{\"purpose\":\"We would like to ensure you have a healthy balance\",\"essential\":false,\"ial\":1,\"assertion\":{\"props\":{\"amount\":{\"gte\":\"500\"},\"currency\":{\"eq\":\"GBP\"}}}}},\"email\":{\"purpose\":\"We will use you email for contact purposes\",\"essential\":false},\"phone_number\":{\"purpose\":\"We will give your phone number to the employeer\",\"essential\":true}}}");
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .audience(dtpConfig.getIssuer())
                .claim("response_type", "code")
                .claim("redirect_uri", redirectURI)
                .claim("nonce", nonce)
                .claim("scope", "openid")
                .claim("state", state.getValue())
                .claim("client_id", clientID.getValue())
                .claim("iss", clientID.getValue())
                .claim("claims", claims)
                .build();

        System.out.println("BODY : " + claimsSet);

        PrivateKeyJWT privateKeyJWT = new PrivateKeyJWT(clientID, new URI(dtpConfig.getIssuer()), JWSAlgorithm.RS256,
                rsaKeyjwk.toRSAPrivateKey(), kid, null);

        // Create RSA-signer with the private key
        JWSSigner signer = new RSASSASigner(rsaKeyjwk);

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid)
                        .type(JOSEObjectType.JWT)
                        .build(),
                claimsSet);

        // Compute the RSA signature
        signedJWT.sign(signer);

        System.out.println("Assertion2: " + signedJWT.serialize());
        System.out.println("PK1: " + privateKeyJWT.getClientAssertion().serialize());

        // Compose the OpenID authentication request (for the code flow)
        AuthenticationRequest request = new AuthenticationRequest.Builder(signedJWT, clientID)
                .customParameter("client_assertion_type", PrivateKeyJWT.CLIENT_ASSERTION_TYPE)
                .customParameter("client_assertion", privateKeyJWT.getClientAssertion().serialize())
                .customParameter("request", signedJWT.serialize())
                .endpointURI(new URI(dtpConfig.getInitiateAuthorizeUrl()))
                .build();

        HTTPRequest requestAuthorize = request.toHTTPRequest(HTTPRequest.Method.POST);
        requestAuthorize.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        HTTPResponse httpResponse = requestAuthorize.send();
        System.out.println("RESPONSE AUTHORIZE: " +  httpResponse.getStatusMessage());
        System.out.println("RESPONSE CONTENT:   " +  httpResponse.getContent() + "\n");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(httpResponse.getContent());

        String redirectUrl = DTPConfig.authorizationEndpointUrl + "?request_uri=" + actualObj.get("request_uri").asText();
        System.out.println("Browse to this url: " + redirectUrl);

        return new RedirectView(redirectUrl);
    }

}
