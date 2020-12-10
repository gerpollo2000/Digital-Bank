package io.digisic.bank.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import io.digisic.bank.config.DTPConfig;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.UUID;

@Service
public class DTPService {

    final DTPConfig dtpConfig;

    public DTPService(DTPConfig dtpConfig) {
        this.dtpConfig = dtpConfig;
    }

    public String validateUser(String redirectURI, String jsonClaims) throws Exception {
        //String redirectURI = dtpConfig.getRedirectURI();
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
        JSONObject claims = (JSONObject) JSONValue.parse(jsonClaims);
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

        return redirectUrl;
    }
}
