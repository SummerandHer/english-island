package com.island.security;

import com.island.common.BusinessException;
import com.island.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IslandUserDetailsService implements UserDetailsService {

	private final UserMapper userMapper;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Long userId = Long.parseLong(username);
		var user = userMapper.selectById(userId);
		if (user == null) {
			throw new UsernameNotFoundException("用户不存在");
		}
		if (user.getStatus() != 1) {
			throw new BusinessException(403, "账号已禁用");
		}
		return new IslandUserDetails(user);
	}
}
