package org.viqueen.devbox.resources;

import com.atlassian.annotations.security.XsrfProtectionExcluded;
import com.atlassian.confluence.jmx.JmxSMTPMailServer;
import com.atlassian.confluence.mail.ConfluencePopMailServer;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.mail.MailConstants;
import com.atlassian.mail.MailException;
import com.atlassian.mail.MailProtocol;
import com.atlassian.mail.server.MailServerManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.user.impl.DefaultUser;
import com.atlassian.user.security.password.Credential;
import com.github.javafaker.Faker;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

@Path("/setup")
public class SetupResource {

    private final MailServerManager mailServerManager;
    private final UserAccessor userAccessor;
    private final SettingsManager settingsManager;

    public SetupResource(
            @ComponentImport MailServerManager mailServerManager,
            @ComponentImport UserAccessor userAccessor,
            @ComponentImport SettingsManager settingsManager) {
        this.mailServerManager = mailServerManager;
        this.userAccessor = userAccessor;
        this.settingsManager = settingsManager;
    }

    @GET
    @Path("/ping")
    public Response ping() {
        return Response.ok(
                singletonMap(
                        "components",
                        asList(
                                mailServerManager.toString(),
                                userAccessor.toString(),
                                settingsManager.toString()
                        )
                )
        ).build();
    }

    @POST
    @Path("/admin")
    @XsrfProtectionExcluded
    public Response admin() {
        ConfluenceUser admin = userAccessor.getUserByName("admin");
        DefaultUser user = new DefaultUser(admin);
        user.setEmail("admin@localhost.test");
        userAccessor.saveUser(user);
        return Response.noContent().build();
    }

    @POST
    @Path("/smtp-server")
    @Produces(MediaType.APPLICATION_JSON)
    @XsrfProtectionExcluded
    public Response outboundMailServer(
            @DefaultValue("outbound-mail-devbox") @QueryParam("name") final String name,
            @DefaultValue("localhost") @QueryParam("hostname") final String hostname,
            @DefaultValue("3025") @QueryParam("port") final String port,
            @DefaultValue("confluence@localhost.test") @QueryParam("from") final String fromAddress,
            @DefaultValue("[local]") @QueryParam("prefix") final String prefix,
            @DefaultValue("confluence") @QueryParam("username") final String username
    ) throws MailException {

        if (mailServerManager.getMailServer(name) != null) {
            return Response.noContent().build();
        }

        final JmxSMTPMailServer smtpMailServer = new JmxSMTPMailServer();
        smtpMailServer.setName(name);
        smtpMailServer.setHostname(hostname);
        smtpMailServer.setPort(port);
        smtpMailServer.setMailProtocol(MailConstants.DEFAULT_SMTP_PROTOCOL);
        smtpMailServer.setDefaultFrom(fromAddress);
        smtpMailServer.setUsername(username);
        smtpMailServer.setPassword(username);
        smtpMailServer.setPrefix(prefix);
        mailServerManager.create(smtpMailServer);

        return Response.ok(smtpMailServer).build();
    }

    @POST
    @Path("/pop-server")
    @Produces(MediaType.APPLICATION_JSON)
    @XsrfProtectionExcluded
    public Response inboundMailServer(
            @DefaultValue("inbound-mail-devbox") @QueryParam("name") final String name,
            @DefaultValue("localhost") @QueryParam("hostname") final String hostname,
            @DefaultValue("3110") @QueryParam("port") final String port,
            @DefaultValue("confluence") @QueryParam("username") final String username,
            @DefaultValue("confluence@localhost.test") @QueryParam("to") final String toAddress
    ) throws MailException {
        if (mailServerManager.getMailServer(name) != null) {
            return Response.noContent().build();
        }

        final ConfluencePopMailServer popMailServer = new ConfluencePopMailServer(
                0L, name, "", MailProtocol.POP, hostname, port, username, username, toAddress
        );

        mailServerManager.create(popMailServer);

        return Response.ok(popMailServer).build();
    }

    @POST
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    @XsrfProtectionExcluded
    public Response createUsers(
            @DefaultValue("1") @QueryParam("start") final int start,
            @DefaultValue("20") @QueryParam("count") final int count
    ) {

        final Map<String, Map<String, String>> users = new HashMap<>();

        IntStream.rangeClosed(start, start + count)
                .forEach(index -> {
                    final Faker faker = LOCALES[index % 23];
                    final String firstName = faker.name().firstName();
                    final String lastName = faker.name().lastName();
                    final String email = format("user%d@localhost.test", index);
                    final String userName = format("user%s%d", (index % 2) == 0 ? "-" : " ", index);

                    final DefaultUser defaultUser = new DefaultUser();
                    defaultUser.setFullName(format("%s %s", firstName, lastName));
                    defaultUser.setEmail(email);
                    defaultUser.setName(userName);

                    final ConfluenceUser confluenceUser = userAccessor.createUser(defaultUser, Credential.unencrypted("user"));

                    userAccessor.addMembership(settingsManager.getGlobalSettings().getDefaultUsersGroup(), userName);

                    final Map<String, String> user = new HashMap<>();
                    user.put("key", confluenceUser.getKey().getStringValue());
                    user.put("initials", format("%s%s", firstName.charAt(0), lastName.charAt(0)));

                    users.put(userName, user);
                });

        return Response.ok(users).build();
    }

    private static final Faker[] LOCALES = new Faker[] {
            Faker.instance(Locale.CANADA),
            Faker.instance(Locale.CANADA_FRENCH),
            Faker.instance(Locale.CHINA),
            Faker.instance(Locale.forLanguageTag("da-DK")),
            Faker.instance(Locale.forLanguageTag("de-AT")),
            Faker.instance(Locale.forLanguageTag("de-CH")),
            Faker.instance(Locale.ENGLISH),
            Faker.instance(Locale.forLanguageTag("es")),
            Faker.instance(Locale.forLanguageTag("es-MX")),
            Faker.instance(Locale.forLanguageTag("fi-FI")),
            Faker.instance(Locale.FRANCE),
            Faker.instance(Locale.GERMANY),
            Faker.instance(Locale.ITALY),
            Faker.instance(Locale.JAPANESE),
            Faker.instance(Locale.KOREA),
            Faker.instance(Locale.forLanguageTag("nb-NO")),
            Faker.instance(Locale.forLanguageTag("nl")),
            Faker.instance(Locale.forLanguageTag("pl")),
            Faker.instance(Locale.forLanguageTag("pt")),
            Faker.instance(Locale.forLanguageTag("ru")),
            Faker.instance(Locale.forLanguageTag("sk")),
            Faker.instance(Locale.forLanguageTag("sv-SE")),
            Faker.instance(Locale.TAIWAN),
            Faker.instance(Locale.UK)
    };
}
