package io.suggest.sec.m

import java.nio.file.{Files, Paths}
import java.security.cert.Certificate
import java.security.{Key, KeyStore}

import io.suggest.sec.util.SecInitUtil
import javax.inject.{Inject, Singleton}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import play.api.Configuration
import play.api.inject.Injector

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.03.19 12:07
  * Description: Утиль для работы с хранилищем ключей sio.
  */
@Singleton
final class MKeyStore @Inject()(
                                 injector   : Injector,
                               )
{

  injector.instanceOf[SecInitUtil]

  private def _configuration = injector.instanceOf[Configuration]


  /** Инстанс хранилища сертификатов и ключей. */
  private lazy val KEYSTORE = {
    val configuration = _configuration
    for {
      conf        <- configuration.getOptional[Configuration]( "sio.sec.keystore" )
      ksPathStr   <- conf.getOptional[String]( "file" )
      ksPath = Paths.get( ksPathStr )
      if Files.exists( ksPath )
      ksPassword  <- conf.getOptional[String]( "password" )
    } yield {
      val ks = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME)

      val is = Files.newInputStream( ksPath )
      try {
        ks.load( is, ksPassword.toCharArray )
      } finally {
        is.close()
      }

      (ks, ksPassword)
    }
  }


  /** Прочитать private key.
    *
    * @param alias Алиас для ключа.
    * @tparam T Тип возвращаемого ключа.
    * @return Some() если ключ найден.
    *         None если ключ отсутствует.
    *         ClassCastException, если тип ключа не соответствует запрашиваемому.
    */
  def getKey[T <: Key](alias: String): Option[T] = {
    for {
      (ks, ksPassword) <- KEYSTORE
      key <- Option( ks.getKey(alias, ksPassword.toCharArray) )
    } yield {
      key.asInstanceOf[T]
    }
  }


  /** Прочитать сертификат.
    *
    * @param alias Алиас.
    * @tparam T Возвращаемый тип сертификата.
    * @return Some() если сертификат есть.
    *         None, если сертификата нет.
    *         ClassCastException, если тип сертификата не соответсвует запрошенному.
    */
  def getCert[T <: Certificate](alias: String): Option[T] = {
    for {
      (ks, _) <- KEYSTORE
      cert <- Option( ks.getCertificate(alias) )
    } yield {
      cert.asInstanceOf[T]
    }
  }

}
