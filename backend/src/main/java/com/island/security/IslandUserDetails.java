package com.island.security;

import com.island.module.user.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class IslandUserDetails implements UserDetails {

	private final User user;

	public IslandUserDetails(User user) {
		this.user = user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		var roles = new java.util.ArrayList<GrantedAuthority>();
		roles.add(new SimpleGrantedAuthority("ROLE_USER"));
		if (user.isAdmin()) {
			roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		}
		if (user.isVipActive()) {
			roles.add(new SimpleGrantedAuthority("ROLE_VIP"));
		}
		return roles;
	}

	@Override
	public String getPassword() {
		return user.getPasswordHash();
	}

	@Override
	public String getUsername() {
		return String.valueOf(user.getId());
	}

	@Override
	public boolean isAccountNonLocked() {
		return user.getStatus() == 1;
	}
}
