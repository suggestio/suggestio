package util.secure

import java.io.{OutputStream, InputStream}

import io.trbl.bcpg.{SecretKey, KeyFactory, KeyFactoryFactory}
import models.sec.{IAsymKey, MAsymKey}
import org.elasticsearch.client.Client
import play.api.Play.{current, configuration}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.04.15 15:22
 * Description: Утиль для простого создания/чтения pgp-сообщений с шифрованием и подписями.
 * Сервера имеют общий pgp-ключ, создаваемый если готового ключа нет в кластерном хранилище.
 * Пароль для секретного ключа хранится в коде.
 *
 * Для PGP используется bcpg-simple, который является враппером над bcpg.
 *
 * Для генерации нового ключа и сохранения в модель используется JMX-команда.
 * При шифровании ключ читается из модели ключей.
 *
 * Normal key -- это ключ защиты простых данных. Тот, кто может отбрутить такой ключ, совсем не интересуется
 * данными, которые защищает данный ключ.
 */
object PgpUtil {

  /** Вся возня с ключами вертится здесь. */
  private val KF: KeyFactory = KeyFactoryFactory.newInstance()

  /** Пароль для секретного ключа сервиса. Можно добавить префикс пароля через конфиг. */
  private val SEC_KEY_PASSWORD: String = {
    val passwordRoot = """Y$mo[@QS-=S!A+W#ZMi;m]l9!,SNC]Ad_(9txd,?jb&"i{O#y'(\!)1yrTsI3m(@"""
    val pwPrefixed = configuration.getString("pgp.key.password.prefix") match {
      case None         => passwordRoot
      case Some(prefix) => prefix + passwordRoot
    }
    pwPrefixed
  }

  private def getPw = SEC_KEY_PASSWORD.toCharArray

  /** Имя документа в модели ключей, где хранится непараноидальный ключ для защиты данных, хранимых в
    * browser.window.localStorage.
    * Изначально был только этот единствнный ключ в модели, и использовался для хранения пользовательских
    * данных в localStorage. */
  val LOCAL_STOR_KEY_ID = "lsk1"

  /** Генерация нового ключа защиты данных. */
  def genNewNormalKey(): MAsymKey = {
    val key = KF.generateKeyPair(LOCAL_STOR_KEY_ID, getPw)
    MAsymKey(
      pubKey = key.getPublicKey.toArmoredString,
      secKey = Some(key.toArmoredString),
      id     = Some(LOCAL_STOR_KEY_ID)
    )
  }

  /** Запустить инициализацию, если задано в конфиге. */
  def maybeInit()(implicit es: Client): Option[Future[_]] = {
    if ( configuration.getBoolean("pgp.keyring.init.enabled").getOrElse(false) ) {
      Some(init())
    } else {
      None
    }
  }

  /** Запустить инициализацию необходимых ключей. */
  def init()(implicit es: Client): Future[_] = {
    MAsymKey.getById(LOCAL_STOR_KEY_ID)
      .filter { _.isDefined }
      .recoverWith { case ex: NoSuchElementException =>
        genNewNormalKey().save
      }
  }

  /**
   * Криптозащита с помощью указанного ключа и для дальнейшей расшифровки этим же ключом.
   * Используется для надежного хранения охраняемых серверных данных на стороне клиента.
   * @param data Входной поток данных.
   * @param key Используемый ASCII-PGP-ключ зашифровки и будущей расшифровки.
   * @param out Куда производить запись?
   */
  def encryptForSelf(data: InputStream, key: IAsymKey, out: OutputStream): Unit = {
    val sc = KF.parseSecretKey(key.secKey.get)
    encrypt(data, sc, key.pubKey, out)
  }

  /**
   * Зашифровка входного потока байт в выходной ASCII-armored поток.
   * @param data Входной поток данных.
   * @param secKey Секретный ключ отправителя (для подписи).
   * @param forPubKey Публичный ключ получателя (для зашифровки).
   * @param out Выходной поток для записи ASCII-armored шифротекста.
   */
  def encrypt(data: InputStream, secKey: SecretKey, forPubKey: String, out: OutputStream): Unit = {
    val transform = secKey.signEncryptFor(forPubKey)
    transform.run(getPw, data, out)
  }


  /**
   * Расшифровать pgp-контейнер, ранее зашифрованный для самого себя.
   * @param data Поток исходных данных.
   * @param key Экземпляр ключа.
   * @param out Выходной поток данных.
   */
  def decryptFromSelf(data: InputStream, key: IAsymKey, out: OutputStream): Unit = {
    val sc = KF.parseSecretKey(key.secKey.get)
    decrypt(data, sc, key.pubKey, out)
  }

  /**
   * Дешифрация данных.
   * @param data Исходный поток данных.
   * @param secKey Секретный ключ для расшифровки.
   * @param signPubKey Публичный ключ для проверки подписи.
   * @param out Выходной поток данных.
   */
  def decrypt(data: InputStream, secKey: SecretKey, signPubKey: String, out: OutputStream): Unit = {
    val transform = secKey.decryptVerifyFrom(signPubKey)
    transform.run(getPw, data, out)
  }

}

