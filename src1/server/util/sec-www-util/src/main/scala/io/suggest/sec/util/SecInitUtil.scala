package io.suggest.sec.util

import java.security.Security

import javax.inject.Singleton
import org.bouncycastle.jce.provider.BouncyCastleProvider

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.03.19 12:10
  * Description: Класс, выполняющий side-effecting-инициализацию системы.
  */
@Singleton
final class SecInitUtil {

  // Constructor
  ensureBcJce()


  // API

  def ensureBcJce() {
    Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) match {
      case null => Security.addProvider(new BouncyCastleProvider)
      case _ => // do nothing
    }
  }

}
