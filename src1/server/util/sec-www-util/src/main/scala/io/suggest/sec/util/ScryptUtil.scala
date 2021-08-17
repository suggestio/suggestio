package io.suggest.sec.util

import javax.inject.Inject
import com.lambdaworks.crypto.{SCryptUtil => UnderlyingScryptUtil}
import io.suggest.util.JmxBase
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Injector, Module}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.16 13:07
  * Description: Утиль для хэширования по scrypt.
  */

// TODO Надо бы переехать на https://github.com/lhunath/scrypt
// com.lambdaworks % scrypt % 1.4.0-lyndir
//
// Можно и bouncycastle, но у неё формат хэша другой, а хеши уже хранятся в БД идентов.
// http://stackoverflow.com/a/41992593

final class ScryptUtil {

  // Настройки генерации хешей. Используется scrypt. Это влияет только на новые создаваемые хеши, не ломая совместимость
  // с уже сохранёнными. Размер потребляемой памяти можно рассчитать Size = (128 * COMPLEXITY * RAM_BLOCKSIZE) bytes.
  // Default values from docs (16384==2^14, 8, 1)==2^24 == 16 MiB RAM, single-thread.
  // Such default gueesed as "scrypt (64 ms)" in https://www.tarsnap.com/scrypt/scrypt.pdf page 14 - table middle.
  // Complexity reduced to 1024, because 16 MiB looks too much to password check, by now.
  /** Cложность хеша scrypt. */
  def SCRYPT_COMPLEXITY     = 1024
  /** Размер блока памяти. */
  def SCRYPT_RAM_BLOCKSIZE  = 8
  /** Параллелизация. Позволяет ускорить вычисление функции. */
  def SCRYPT_PARALLEL       = 1

  /** Генерировать новый хеш с указанными выше дефолтовыми параметрами.
    *
    * @param password Пароль, который надо захешировать.
    * @return Текстовый хеш в стандартном формате \$s0\$params\$salt\$key.
    */
  def mkHash(password: String): String = {
    UnderlyingScryptUtil.scrypt(password, SCRYPT_COMPLEXITY, SCRYPT_RAM_BLOCKSIZE, SCRYPT_PARALLEL)
  }

  /** Проверить хеш scrypt с помощью переданного пароля.
    *
    * @param password Проверяемый пароль.
    * @param hash Уже готовый хеш.
    * @return true, если пароль ок. Иначе false.
    */
  def checkHash(password: String, hash: String): Boolean = {
    UnderlyingScryptUtil.check(password, hash)
  }

  /** Запрещаем бородатому scrypt'у грузить в jvm нативную amd64-либу, ибо она взрывоопасна без перекомпиляции
    * под свежие libcrypto.so (пакет openssl):
    *
    * Native frames: (J=compiled Java code, j=interpreted, Vv=VM code, C=native code)
    * C  [libcrypto.so.1.0.0+0x6c1d7]  SHA256_Update+0x157
    *
    * Java frames: (J=compiled Java code, j=interpreted, Vv=VM code)
    *   com.lambdaworks.crypto.SCrypt.scryptN([B[BIIII)[B+0
    *   com.lambdaworks.crypto.SCrypt.scrypt([B[BIIII)[B+14
    *   com.lambdaworks.crypto.SCryptUtil.check(Ljava/lang/String;Ljava/lang/String;)Z+118
    * @see com.lambdaworks.jni.LibraryLoaders.loader().
    */
  def disableNativeCode(): Unit = {
    val scryptJniProp = "com.lambdaworks.jni.loader"
    if (System.getProperty(scryptJniProp) != "nil")
      System.setProperty(scryptJniProp, "nil")
  }

}

/** Интерфейс для JMX MBean'а для [[ScryptUtil]]. */
trait SCryptUtilJmxMBean {

  def mkHash(password: String): String

  def checkPasswordHash(password: String, hash: String): Boolean

}


/** Реализация JMX Mbean'а [[SCryptUtilJmx]] для [[ScryptUtil]]. */
class SCryptUtilJmx @Inject() (
                                injector        : Injector
                              )
  extends JmxBase
  with SCryptUtilJmxMBean
{
  import JmxBase._

  override def _jmxType: String = Types.UTIL

  private def scryptUtil = injector.instanceOf[ScryptUtil]

  override def mkHash(password: String): String = {
    scryptUtil.mkHash(password)
  }

  override def checkPasswordHash(password: String, hash: String): Boolean = {
    scryptUtil.checkHash(password, hash = hash)
  }

}
