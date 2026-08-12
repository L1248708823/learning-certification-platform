package com.learningplatform.iam.user;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.iam.enabled", havingValue = "true", matchIfMissing = true)
public class IamUserDetailsService implements UserDetailsService {

    private final UserAccountMapper userMapper;

    public IamUserDetailsService(UserAccountMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccountEntity user = userMapper.findByUsername(username);
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new UsernameNotFoundException(ErrorCode.USER_NOT_AVAILABLE.getDefaultMessage());
        }
        return toPrincipal(user);
    }

    public IamUserPrincipal loadActivePrincipal(String username) {
        UserAccountEntity user = userMapper.findByUsername(username);
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_NOT_AVAILABLE);
        }
        return toPrincipal(user);
    }

    private IamUserPrincipal toPrincipal(UserAccountEntity user) {
        return new IamUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getStatus(),
                userMapper.findRoleCodes(user.getId()));
    }
}
