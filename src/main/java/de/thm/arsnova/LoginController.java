/*
 * Copyright (C) 2012 THM webMedia
 * 
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.scribe.up.provider.impl.FacebookProvider;
import org.scribe.up.provider.impl.Google2Provider;
import org.scribe.up.provider.impl.TwitterProvider;
import org.scribe.up.session.HttpUserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import de.thm.arsnova.entities.User;
import de.thm.arsnova.services.IUserService;

@Controller
public class LoginController {

	@Autowired
	TwitterProvider twitterProvider;
	
	@Autowired
	Google2Provider googleProvider;
	
	@Autowired
	FacebookProvider facebookProvider;
	
	@Autowired
	CasAuthenticationEntryPoint casEntryPoint;
	
	@Autowired
	IUserService userService;
	
	public static final Logger logger = LoggerFactory
			.getLogger(LoginController.class);

	@RequestMapping(method = RequestMethod.GET, value = "/doLogin")
	public ModelAndView doLogin(@RequestParam("type") String type, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		if("cas".equals(type)) {
			casEntryPoint.commence(request, response, null);
		} else if("twitter".equals(type)) {
			String authUrl = twitterProvider.getAuthorizationUrl(new HttpUserSession(request));
			return new ModelAndView("redirect:" + authUrl);
		} else if("facebook".equals(type)) {
			String authUrl = facebookProvider.getAuthorizationUrl(new HttpUserSession(request));
			return new ModelAndView("redirect:" + authUrl);
		} else if("google".equals(type)) {
			String authUrl = googleProvider.getAuthorizationUrl(new HttpUserSession(request));
			return new ModelAndView("redirect:" + authUrl);
		} else if("guest".equals(type)) {
			List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
			authorities.add(new SimpleGrantedAuthority("ROLE_GUEST"));
			String username = "Guest" + Sha512DigestUtils.shaHex(request.getSession().getId()).substring(0, 10);
			org.springframework.security.core.userdetails.User user = 
					new org.springframework.security.core.userdetails.User(username, "", true, true, true, true, authorities);
			Authentication token = new UsernamePasswordAuthenticationToken(user, null, authorities);

			SecurityContextHolder.getContext().setAuthentication(token);
			request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
			return new ModelAndView("redirect:/#auth/checkLogin");
		}
		return null;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/whoami")
	public User whoami() {
		return userService.getUser(SecurityContextHolder.getContext().getAuthentication());
	}

	@RequestMapping(method = RequestMethod.GET, value = "/logout")
	public ModelAndView doLogout(final HttpServletRequest request) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		request.getSession().invalidate();
		if (auth instanceof CasAuthenticationToken) {
			return new ModelAndView("redirect:/j_spring_cas_security_logout");
		}
		return new ModelAndView("redirect:/");
	}
}
