package io.suggest.xplay.qsb

import io.suggest.util.logs.IMacroLogs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.16 23:03
  * Description: Бывает, что нужно зафиксировать ключ константой.
  */
trait QsbFixedKeyOuter extends IMacroLogs {

  /** Фиксированное значение key. */
  protected def _FIXED_KEY: String

  /** Трейт, подмешиваемый в реализацию QSB для получания утили фиксированного ключа. */
  protected trait QsbFixedKey extends QsbKey1T {

    /** Генерация имени под-поля на основе фиксированного ключа. */
    def k(suf: String) = key1(_FIXED_KEY, suf)

    /** Ругаться в логи, если в routes задано имя параметра отличное от прописанного в расшаренном Constants. */
    def _checkQsKey(key: String): Unit = {
      val k0 = _FIXED_KEY
      if (key != k0)
        LOGGER.warn(s"_checkQsKey(): key '$key' != '$k0'. Ensure valid param name in routes!")
    }

  }

}
