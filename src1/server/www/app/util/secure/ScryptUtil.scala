package util.secure

import com.google.inject.{Inject, Singleton}
import com.lambdaworks.crypto.SCryptUtil
import io.suggest.util.JMXBase

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.16 13:07
  * Description: Утиль для хэширования по scrypt.
  */
@Singleton
class ScryptUtil {

  // Конструктор класса.
  // Запретить использование нативных либ. См.коммент к disableNativeCode().
  disableNativeCode()


  // Настройки генерации хешей. Используется scrypt. Это влияет только на новые создаваемые хеши, не ломая совместимость
  // с уже сохранёнными. Размер потребляемой памяти можно рассчитать Size = (128 * COMPLEXITY * RAM_BLOCKSIZE) bytes.
  // По дефолту жрём 16 метров с запретом параллелизации.
  /** Cложность хеша scrypt. */
  def SCRYPT_COMPLEXITY     = 16384 //current.configuration.getInt("ident.pw.scrypt.complexity") getOrElse
  /** Размер блока памяти. */
  def SCRYPT_RAM_BLOCKSIZE  = 8 //current.configuration.getInt("ident.pw.scrypt.ram.blocksize") getOrElse 8
  /** Параллелизация. Позволяет ускорить вычисление функции. */
  def SCRYPT_PARALLEL       = 1 //current.configuration.getInt("ident.pw.scrypt.parallel") getOrElse 1

  /** Генерировать новый хеш с указанными выше дефолтовыми параметрами.
    *
    * @param password Пароль, который надо захешировать.
    * @return Текстовый хеш в стандартном формате \$s0\$params\$salt\$key.
    */
  def mkHash(password: String): String = {
    SCryptUtil.scrypt(password, SCRYPT_COMPLEXITY, SCRYPT_RAM_BLOCKSIZE, SCRYPT_PARALLEL)
  }

  /** Проверить хеш scrypt с помощью переданного пароля.
    *
    * @param password Проверяемый пароль.
    * @param hash Уже готовый хеш.
    * @return true, если пароль ок. Иначе false.
    */
  def checkHash(password: String, hash: String): Boolean = {
    SCryptUtil.check(password, hash)
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
  def disableNativeCode() {
    val scryptJniProp = "com.lambdaworks.jni.loader"
    if (System.getProperty(scryptJniProp) != "nil")
      System.setProperty(scryptJniProp, "nil")
  }

}

/** Интерфейс для DI-поля с инстансом [[ScryptUtil]]. */
trait IScryptUtilDi {
  def scryptUtil: ScryptUtil
}


/** Интерфейс для JMX MBean'а для [[ScryptUtil]]. */
trait SCryptUtilJmxMBean {

  def mkHash(password: String): String

  def checkPasswordHash(password: String, hash: String): Boolean

}


/** Реализация JMX Mbean'а [[SCryptUtilJmx]] для [[SCryptUtil]]. */
class SCryptUtilJmx @Inject() (
                                scryptUtil: ScryptUtil
                              )
  extends JMXBase
  with SCryptUtilJmxMBean
{

  override def jmxName = "io.suggest:type=util,name=" + getClass.getSimpleName.replace("Jmx", "")

  override def mkHash(password: String): String = {
    scryptUtil.mkHash(password)
  }

  override def checkPasswordHash(password: String, hash: String): Boolean = {
    scryptUtil.checkHash(password, hash = hash)
  }

}
