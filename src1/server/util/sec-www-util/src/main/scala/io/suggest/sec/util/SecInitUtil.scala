package io.suggest.sec.util

import java.security.Security

import org.bouncycastle.jce.provider.BouncyCastleProvider

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.03.19 12:10
  * Description: Класс, выполняющий side-effecting-инициализацию системы.
  */
final class SecInitUtil {

  /** CipherUtil:
    * PKCS7Padding only works ok in BouncyCastle (java-8), so it really need to register BC JCE-provider
    * when application starts (and also before tests).
    */
  def ensureBcJce(): Unit = {
    Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) match {
      case null => Security.addProvider(new BouncyCastleProvider)
      case _ => // do nothing
    }
  }

}
