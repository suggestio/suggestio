package util.secure

import java.io.{OutputStream, InputStream}

import com.google.inject.{Inject, Singleton}
import io.trbl.bcpg.{SecretKey, KeyFactory, KeyFactoryFactory}
import models.mproj.ICommonDi
import models.sec.{IAsymKey, MAsymKey}
import util.PlayMacroLogsDyn

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
@Singleton
class PgpUtil @Inject() (
  mCommonDi   : ICommonDi
)
  extends PlayMacroLogsDyn
{

  import mCommonDi._

  /** Вся возня с ключами вертится здесь. */
  private val KF: KeyFactory = KeyFactoryFactory.newInstance()

  /** Пароль для секретного ключа сервиса. Можно добавить префикс пароля через конфиг. */
  private val SEC_KEY_PASSWORD: String = {
    val passwordRoot = """Y$mo[@QS-=S!A+W#ZMi;m]l9!,SNC]Ad_(9txd,?jb&"i{O#y'(\!)1yrTsI3m(@"""
    configuration.getString("pgp.key.password.prefix")
      .fold (passwordRoot) { _ + passwordRoot }
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
  def maybeInit(): Option[Future[_]] = {
    val cfk = "pgp.keyring.init.enabled"
    if ( configuration.getBoolean(cfk).getOrElse(false) ) {
      Some(init())
    } else {
      MAsymKey.getById(LOCAL_STOR_KEY_ID)
        // TODO проверять, что пароль соответствует ключу. Нужно пытаться зашифровать какие-то простые данные.
        .filter { _.isDefined }
        .onFailure {
          case ex: NoSuchElementException => LOGGER.warn("PGP key does not exists and creation is disabled on this node: " + cfk)
          case ex: Throwable              => LOGGER.error("Failed to check status of server's pgp key.", ex)
        }
      None
    }
  }

  /** Запустить инициализацию необходимых ключей. */
  def init(): Future[_] = {
    val fut = MAsymKey.getById(LOCAL_STOR_KEY_ID)
      .filter { _.isDefined }
      .recoverWith { case ex: NoSuchElementException =>
        genNewNormalKey().save
      }
    fut.onFailure {
      case ex: Throwable =>
        LOGGER.error("Failed to initialize server's PGP key", ex)
    }
    fut
  }


  /**
   * Криптозащита с помощью указанного ключа и для дальнейшей расшифровки этим же ключом.
   * Используется для надежного хранения охраняемых серверных данных на стороне клиента.
    *
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
    *
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
    *
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
    *
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

