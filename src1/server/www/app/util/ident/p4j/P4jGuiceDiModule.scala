package util.ident.p4j

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import controllers.routes
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.p4j.P4jWebContextFactory
import org.pac4j.core.client.{Client, Clients}
import org.pac4j.core.credentials.Credentials
import org.pac4j.core.profile.CommonProfile
import org.pac4j.oidc.client.OidcClient
import org.pac4j.oidc.config.OidcConfiguration
import org.pac4j.oidc.profile.OidcProfile
import org.pac4j.play.store.{PlayCookieSessionStore, PlaySessionStore}
import org.pac4j.core.config.{Config => P4jConfig}
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.02.19 18:48
  * Description: Поддержка DI для Pac4j-утили.
  */
class P4jGuiceDiModule extends AbstractModule {

  override def configure(): Unit = {
    // Жесткий stateless для всех запросов:
    bind(classOf[PlaySessionStore]) to classOf[PlayCookieSessionStore]

    // Система сборки web-контекстов:
    install(
      new FactoryModuleBuilder()
        .build( classOf[P4jWebContextFactory] )
    )
  }

}


/** На уровне Guice-модуля не сработал Configuration, поэтому pj-конфиг пилим на уровне play-модуля. */
class P4jDiModule extends Module with MacroLogsImpl {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    _mkBindPjConfigBind( configuration ) ::
      Nil
  }

  /** Сборка опционального биндинга для P4J-конфига. */
  private def _mkBindPjConfigBind(configuration: Configuration): Binding[P4jConfig] = {
    var clientsAcc = List.empty[Client[_ <: Credentials, _ <: CommonProfile]]

    val P4J_ROOT_KEY = "p4j"
    // Биндинг всего остального: конфига, клиентуры и т.д.
    // Взять весь конфиг p4j.
    val p4jConfigOpt = configuration.getOptional[Configuration]( P4J_ROOT_KEY )
    if (p4jConfigOpt.isEmpty)
      LOGGER.info("Missing all p4j configuration")

    val P4J_OIDC_KEY = "oidc"
    val rootConfigPrefix = s"${P4J_ROOT_KEY}.${P4J_OIDC_KEY}"

    // Инициализация OpenID Connect:
    for {
      p4jConfig <- p4jConfigOpt

      // Отработать конфиг OpenID Connect:
      oidcConfig <- {
        val oidcConfigOpt = p4jConfig.getOptional[Configuration](P4J_OIDC_KEY)
        if (oidcConfigOpt.isEmpty)
          LOGGER.info(s"Missing OIDC configuration: $rootConfigPrefix = {...}")
        oidcConfigOpt
      }

      discoveryUrl <- {
        val key = "discoveryUri"
        val discoveryUrlOpt = oidcConfig.getOptional[String](key)
        if (discoveryUrlOpt.isEmpty)
          LOGGER.error(s"Missing config entry: $rootConfigPrefix.$key = https://...")
        discoveryUrlOpt
      }

      clientKey = "client"
      clientRootKey = s"$rootConfigPrefix.$clientKey"
      clientConfig <- {
        val clientConfOpt = oidcConfig.getOptional[Configuration](clientKey)
        if (clientConfOpt.isEmpty)
          LOGGER.error(s"Missing client config: $clientRootKey = { ... }")
        clientConfOpt
      }

      clientId <- {
        val idKey = "id"
        val clientIdOpt = clientConfig.getOptional[String](idKey)
        if (clientIdOpt.isEmpty)
          LOGGER.error(s"Missing config entry: $rootConfigPrefix.$idKey = asd123...")
        clientIdOpt
      }

      clientSecret <- {
        val secretConfKey = "secret"
        val secretOpt = clientConfig.getOptional[String](secretConfKey)
        if (secretOpt.isEmpty)
          LOGGER.error(s"Missing client secret in config: $rootConfigPrefix.$secretConfKey = zxc098...")
        secretOpt
      }

    } {
      val p4jOidcConf = new OidcConfiguration()
      p4jOidcConf.setDiscoveryURI( discoveryUrl )
      p4jOidcConf.setClientId( clientId )
      p4jOidcConf.setSecret( clientSecret )

      val p4jOidcClient = new OidcClient[OidcProfile,OidcConfiguration]( p4jOidcConf )
      // В примерах кодах здесь вызывается p4jOidcClient.addAuthorizationGenerator(), который делает profile.addRole("ROLE_ADMIN")

      clientsAcc ::= p4jOidcClient
    }

    // После всех клиентом - инициализация p4j-конфига с клиентами:
    val clients = new Clients(
      routes.Ident.extIdCbGet().url,
      clientsAcc : _*
    )
    val p4jConfig = new P4jConfig( clients )
    // setActionAdapter() - здесь пропускаем: все Result'ы отрабатываются на усмотрение контроллера.

    bind( classOf[P4jConfig] ) toInstance p4jConfig
  }

}

