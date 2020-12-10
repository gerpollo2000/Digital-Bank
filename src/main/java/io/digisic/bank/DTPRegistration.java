package io.digisic.bank;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.client.ClientRegistrationErrorResponse;
import com.nimbusds.oauth2.sdk.client.ClientRegistrationResponse;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderConfigurationRequest;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.rp.*;
import io.digisic.bank.config.DTPConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;

@Component
public class DTPRegistration implements CommandLineRunner, Ordered {

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public void run(String... args) throws Exception {
        try {

            DTPConfig dtpConfig = new DTPConfig();

            String redirectURI = DTPConfig.redirectURI;
            String issuer = dtpConfig.getIssuer();
            String kid = dtpConfig.getKid();

            //Retrieve DTP Endpoints
            URI issuerURI = new URI(issuer);

            URL providerConfigurationURL = issuerURI.resolve(OIDCProviderConfigurationRequest.OPENID_PROVIDER_WELL_KNOWN_PATH).toURL();
            InputStream stream = providerConfigurationURL.openStream();
            // Read all data from URL
            String providerInfo = null;
            try (java.util.Scanner s = new java.util.Scanner(stream)) {
                providerInfo = s.useDelimiter("\\A").hasNext() ? s.next() : "";
            }
            OIDCProviderMetadata providerMetadata = OIDCProviderMetadata.parse(providerInfo);

            System.out.println("===================================================");
            System.out.println("PROVIDER INFO: " + providerInfo);
            System.out.println("===================================================");

            //Store authorization endpoint
            DTPConfig.authorizationEndpointUrl = providerMetadata.getAuthorizationEndpointURI().toString();
            // Generate 2048-bit RSA key pair in JWK format, attach some metadata
            RSAKey rsaKeyjwk = new RSAKeyGenerator(2048)
                    .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
                    .keyID(kid) // give the key a unique ID
                    .generate();

            RSAKey rsaPublicJWK = rsaKeyjwk.toPublicJWK();

            //Store RSAKey
            DTPConfig.RSAKey = rsaKeyjwk;

            // Output the private and public RSA JWK parameters
            System.out.println("PRIVATE KEY = " + rsaKeyjwk);

            // Output the public RSA JWK parameters only
            System.out.println("PUBLIC KEY = " + rsaPublicJWK);

            //REGISTER APPLICATION
            OIDCClientMetadata metadata = new OIDCClientMetadata();
            metadata.setName("DigitalBank");
            metadata.setLogoURI(new URI(dtpConfig.getLogoUrl()));
            metadata.setPolicyURI(new URI(dtpConfig.getPrivacyPolicyUrl()));
            metadata.setTermsOfServiceURI(new URI(dtpConfig.getTermsOfServiceUrl()));
            metadata.setRedirectionURI(new URI(redirectURI));
            metadata.setApplicationType(ApplicationType.WEB);
            metadata.setTokenEndpointAuthMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT);
            metadata.setTokenEndpointAuthJWSAlg(JWSAlgorithm.RS256);
            metadata.setJWKSet(new JWKSet(rsaPublicJWK));

            // Make registration request
            OIDCClientRegistrationRequest registrationRequest = new OIDCClientRegistrationRequest(providerMetadata.getRegistrationEndpointURI(),
                    metadata, null);
            HTTPResponse regHTTPResponse = registrationRequest.toHTTPRequest().send();

            // Parse and check response
            ClientRegistrationResponse registrationResponse = OIDCClientRegistrationResponseParser.parse(regHTTPResponse);

            if (registrationResponse instanceof ClientRegistrationErrorResponse) {
                ErrorObject error = ((ClientRegistrationErrorResponse) registrationResponse).getErrorObject();
                // TODO error handling
                System.err.println("OOOOOOOOOOPSSSSS A BIG PROBLEM HAPPENED!!!!!!! " + error.getDescription());
            }

            // Store client information from OP
            OIDCClientInformation clientInformation = ((OIDCClientInformationResponse)registrationResponse).getOIDCClientInformation();

            //SEND THE AUTHENTICATION REQUEST

            // The client ID provisioned by the OpenID provider when
            // the client was registered
            ClientID clientID = new ClientID(clientInformation.getID());

            //Store cliendId
            DTPConfig.clientID = clientID;

            PrivateKeyJWT privateKeyJWT = new PrivateKeyJWT(clientID, new URI(providerMetadata.getIssuer().getValue()), JWSAlgorithm.RS256,
                    rsaKeyjwk.toRSAPrivateKey(), kid, null);

            //Store the private key in the Config bean for later use
            DTPConfig.privateKeyJWT = privateKeyJWT;

            System.out.println("*** Application successfully REGISTERED in Santander DT. ClientID: " + clientID + " ***");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
