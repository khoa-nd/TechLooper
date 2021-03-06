package com.techlooper.controller;

import com.techlooper.entity.VnwUserProfile;
import com.techlooper.model.Language;
import com.techlooper.model.SocialConfig;
import com.techlooper.model.SocialProvider;
import com.techlooper.repository.JsonConfigRepository;
import com.techlooper.service.UserService;
import com.techlooper.service.VietnamWorksUserService;
import com.techlooper.service.impl.FacebookService;
import com.techlooper.service.impl.GoogleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.social.connect.UserProfile;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by phuonghqh on 12/10/14.
 */
@Controller
public class SocialController {

  private static final Logger LOGGER = LoggerFactory.getLogger(SocialController.class);

//  @Resource
//  private ApplicationContext applicationContext;

  @Resource
  private JsonConfigRepository jsonConfigRepository;

//  @Resource
//  private TextEncryptor textEncryptor;

  @Resource
  private FacebookService facebookService;

  @Resource
  private GoogleService googleService;

  @Resource
  private VietnamWorksUserService vietnamWorksUserService;

  @Resource
  private UserService userService;

  @RequestMapping(value = "login/social/{social}", method = RequestMethod.GET)
  public void loginBySocial(@PathVariable SocialProvider social, @RequestParam(required = false) String code,
                            HttpServletResponse response) throws IOException {
    response.sendRedirect(code == null ? "/#/?action=cancel-social-register" :
      String.format("/#/?action=loginBySocial&social=%s&code=%s", social, code));
  }

  @RequestMapping(value = "register/vnw/google", method = RequestMethod.GET)
  public void registerVnwUserFromGoogle(@RequestParam(required = false) String state, @RequestParam(required = false) String code,
                                        HttpServletResponse response) throws IOException {
    if (code == null) {
      response.sendRedirect("/#/?action=cancel-social-register");
      return;
    }

    SocialConfig socialConfig = jsonConfigRepository.getSocialConfig().stream()
      .filter(config -> SocialProvider.GOOGLE == config.getProvider()).findFirst().get();
    UserProfile userProfile;
    try {
      userProfile = googleService.getUserProfile(code, socialConfig);
    }
    catch (Exception e) {
      response.sendRedirect(socialConfig.getApiUrl().get("login"));
      return;
    }

    sendOnBoardingEmail(userProfile.getEmail(), state);
    registerVnwUserAndRedirect(response, userProfile.getFirstName(), userProfile.getLastName(), userProfile.getEmail());
  }

  private void sendOnBoardingEmail(String email, String language) throws IOException {
    if (!StringUtils.hasText(email)) return;

    try {
      userService.sendOnBoardingEmail(email, Language.parse(language));
    }
    catch (Exception e) {
      LOGGER.debug("Not send on boarding email", e);
    }
  }

  private void registerVnwUserAndRedirect(HttpServletResponse response, String firstName, String lastName, String email) throws IOException {
    if (StringUtils.hasText(email)) {
      try {
        vietnamWorksUserService.register(VnwUserProfile.VnwUserProfileBuilder.vnwUserProfile()
          .withEmail(email).withFirstname(firstName).withLastname(lastName).build());
      }
      catch (Exception e) {
        LOGGER.debug("Error register Vietnamworks", e);
      }
    }

    response.sendRedirect(String.format("/#/?action=registerVnwUser&firstName=%s&lastName=%s&email=%s",
      URLEncoder.encode(firstName, "UTF-8"), URLEncoder.encode(lastName, "UTF-8"), email));
//    if (StringUtils.hasText(email)) {
//      response.sendRedirect(String.format("/#/?action=registerVnwUser&firstName=%s&lastName=%s&email=%s",
//        URLEncoder.encode(firstName, "UTF-8"), URLEncoder.encode(lastName, "UTF-8"), email));
//    }
//    else {
//      response.sendRedirect(String.format("/#/?action=registerVnwUser&firstName=%s&lastName=%s",
//        URLEncoder.encode(firstName, "UTF-8"), URLEncoder.encode(lastName, "UTF-8")));
//    }
  }


  @RequestMapping(value = "register/vnw/fb", method = RequestMethod.GET)
  public void registerVnwUserFromFB(@RequestParam(required = false) String state, @RequestParam(required = false) String code, HttpServletResponse response) throws IOException {
    if (code == null) {
      response.sendRedirect("/#/?action=cancel-social-register");
      return;
    }

    SocialConfig socialConfig = jsonConfigRepository.getSocialConfig().stream()
      .filter(config -> SocialProvider.FACEBOOK_REGISTER == config.getProvider()).findFirst().get();

    UserProfile userProfile;
    try {
      userProfile = facebookService.getUserProfile(code, socialConfig);
    }
    catch (Exception e) {
      response.sendRedirect(socialConfig.getApiUrl().get("login"));
      return;
    }

    sendOnBoardingEmail(userProfile.getEmail(), state);
    registerVnwUserAndRedirect(response, userProfile.getFirstName(), userProfile.getLastName(), userProfile.getEmail());
  }

  @ResponseBody
  @RequestMapping(value = "social/{provider}/loginUrl", method = RequestMethod.GET)
  public String getSocialLoginUrl(@PathVariable SocialProvider provider) {
    SocialConfig socialConfig = jsonConfigRepository.getSocialConfig().stream()
      .filter(config -> provider == config.getProvider()).findFirst().get();
    return socialConfig.getApiUrl().get("login");
  }


  @ResponseBody
  @RequestMapping("/getSocialConfig")
  public List<SocialConfig> getSocialConfig(@RequestParam("providers[]") List<SocialProvider> providers) {
    List<SocialConfig> configs = new ArrayList<>();
    providers.forEach(prov ->
      configs.add(jsonConfigRepository.getSocialConfig().stream()
        .filter(config -> prov == config.getProvider()).findFirst().get())
    );
    return configs;
  }

//  @ResponseBody
//  @RequestMapping("/auth/{provider}")
//  public SocialResponse auth(@PathVariable SocialProvider provider,
//                             @CookieValue(value = "techlooper.key", required = false) String key,
//                             @RequestBody(required = false) Authentication auth) {
//    SocialService service = applicationContext.getBean(provider + "Service", SocialService.class);
//    AccessGrant accessGrant = service.getAccessGrant(auth.getCode());
//    UserEntity userEntity = StringUtils.hasText(key) ? service.saveFootprint(accessGrant, key) : service.saveFootprint(accessGrant);
//    return SocialResponse.Builder.get()
//      .withToken(accessGrant.getAccessToken())
//      .withKey(textEncryptor.encrypt(userEntity.key())).build();
//  }
//
//  @ResponseBody
//  @RequestMapping("/auth/oath1/{provider}")
//  public SocialResponse auth1(@PathVariable SocialProvider provider, HttpServletResponse httpServletResponse,
//                              @CookieValue(value = "techlooper.key", required = false) String key,
//                              @RequestParam(value = "oauth_token", required = false) String token,
//                              @RequestParam(value = "oauth_verifier", required = false) String verifier) throws IOException {
//    SocialService service = applicationContext.getBean(provider + "Service", SocialService.class);
//    if (Optional.ofNullable(token).isPresent()) {
//      AccessGrant accessGrant = service.getAccessGrant(token, verifier);
//      UserEntity userEntity = StringUtils.hasText(key) ? service.saveFootprint(accessGrant, key) : service.saveFootprint(accessGrant);
//      return SocialResponse.Builder.get()
//        .withToken(accessGrant.getValue())
//        .withKey(textEncryptor.encrypt(userEntity.key())).build();
//    }
//    AccessGrant accessGrant = service.getAccessGrant(null, null);
//    httpServletResponse.sendRedirect(accessGrant.getAuthorizeUrl());
//    return null;
//  }
}