package io.digisic.bank.controller.web;

import java.security.Principal;
import java.util.List;

import io.digisic.bank.config.DTPConfig;
import io.digisic.bank.model.security.Users;
import io.digisic.bank.service.DTPService;
import io.digisic.bank.service.UserService;
import io.digisic.bank.util.Messages;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.digisic.bank.service.VisaService;
import io.digisic.bank.util.Constants;
import io.digisic.bank.util.Patterns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping(Constants.URI_XFER_EXTERNAL)
public class WebVisaController extends WebCommonController {
	
	private static final Logger LOG = LoggerFactory.getLogger(VisaService.class);

	@Autowired
	private VisaService visaService;

	@Autowired
	private DTPService dtpService;

	@Autowired
	private UserService userService;

	@GetMapping(Constants.URI_XFER_VISA)
	public String checkingAdd(Principal principal, Model model) {
		
		// Set Display Defaults
		setDisplayDefaults(principal, model);
		
		// Add format patterns
		model.addAttribute(MODEL_ATT_PATTERN_TRANS_AMOUNT, Patterns.TRANSACTION_AMOUNT);
		model.addAttribute(MODEL_ATT_EXT_ACCOUNT, new String());
		model.addAttribute(MODEL_ATT_EXT_AMOUNT, new String());

		return Constants.VIEW_XFER_VISA;
	}


	@PostMapping(Constants.URI_XFER_VISA_PROC)
	public String transferAuthenticateUser(Principal principal, Model model,
												 @ModelAttribute(MODEL_ATT_EXT_ACCOUNT) String extAccount,
												 @ModelAttribute(MODEL_ATT_EXT_AMOUNT) String extAmount) {

		// Set Display Defaults
		setDisplayDefaults(principal, model);

		Users user = userService.findByUsername(principal.getName());

		//user = userService.updateUserProfile(user, updateProfile);

		if (user.getUserProfile().isVerified()) {
			model.addAttribute(MODEL_ATT_SUCCESS_MSG, "Your transfer has been processed! 🎉💰");
		} else {
			model.addAttribute(MODEL_ATT_ERROR_MSG, "You're not allowed to do transfers with an unverified profile ☹");
		}

		return Constants.VIEW_XFER_VISA;
	}
}