package pl.kordelia.web;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public final class AgentIdentity {
    private AgentIdentity() {}

    public static void runAs(Runnable action) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_AGENT_AI"));
        var authentication = new UsernamePasswordAuthenticationToken("agent_ai", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
