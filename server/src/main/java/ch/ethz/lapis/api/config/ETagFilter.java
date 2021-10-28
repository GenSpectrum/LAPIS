package ch.ethz.lapis.api.config;

import ch.ethz.lapis.api.DataVersionService;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ETagFilter extends OncePerRequestFilter {

    private final DataVersionService dataVersionService;

    public ETagFilter(DataVersionService dataVersionService) {
        this.dataVersionService = dataVersionService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String etagWithQuotes = request.getHeader("If-None-Match");
        if (etagWithQuotes != null) {
            String etag = etagWithQuotes.substring(1, etagWithQuotes.length() - 1); // ETags are always in quotes
            String[] split = etag.split("-");
            if (split.length == 2) {
                String dataVersionString = split[1];
                if (dataVersionString.equals(String.valueOf(dataVersionService.getVersion()))) {
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
