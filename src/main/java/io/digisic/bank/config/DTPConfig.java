package io.digisic.bank.config;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import io.digisic.bank.util.Constants;
import org.springframework.stereotype.Component;

@Component
public class DTPConfig {

    //public static final String appBaseUrl = "http://localhost:8080/bank";
    public static final String appBaseUrl = "http://dtpbank.co:8080/bank";
    public static final String redirectURI = appBaseUrl + Constants.URI_USER + Constants.URI_USR_PROFILE_AUTHENTICATE;
    private String issuer = "https://op.iamid.io";
    private String kid = "DigitalBankKey";
    private String logoUrl = "https://raw.githubusercontent.com/gerpollo2000/Digital-Bank/master/images/DBLogo650.jpg";
    private String privacyPolicyUrl = "https://www.santanderlabs.io/en/privacy-policy";
    private String termsOfServiceUrl = "https://www.santanderlabs.io/en/tos";
    private String appName = "DigitalBank";
    private String initiateAuthorizeUrl = "https://live.iamid.io/initiate-authorize";
    private String tokenUrl = "https://live.iamid.io/token";
    public static PrivateKeyJWT privateKeyJWT;
    public static RSAKey RSAKey;
    public static ClientID clientID;
    public static String authorizationEndpointUrl;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getPrivacyPolicyUrl() {
        return privacyPolicyUrl;
    }

    public void setPrivacyPolicyUrl(String privacyPolicyUrl) {
        this.privacyPolicyUrl = privacyPolicyUrl;
    }

    public String getTermsOfServiceUrl() {
        return termsOfServiceUrl;
    }

    public void setTermsOfServiceUrl(String termsOfServiceUrl) {
        this.termsOfServiceUrl = termsOfServiceUrl;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getInitiateAuthorizeUrl() {
        return initiateAuthorizeUrl;
    }

    public void setInitiateAuthorizeUrl(String initiateAuthorizeUrl) {
        this.initiateAuthorizeUrl = initiateAuthorizeUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }
}
