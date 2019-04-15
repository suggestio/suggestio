package io.suggest.sec.util

import java.io.{InputStream, OutputStream}

import javax.inject.{Inject, Singleton}
import io.suggest.es.model.EsModel
import io.suggest.sec.m.{IAsymKey, MAsymKey, MAsymKeys}
import io.suggest.util.logs.MacroLogsDyn
import io.trbl.bcpg.{KeyFactory, KeyFactoryFactory, SecretKey}
import io.suggest.common.empty.OptionUtil.BoolOptOps
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

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
                          esModel                 : EsModel,
                          mAsymKeys               : MAsymKeys,
                          configuration           : Configuration,
                          implicit private val ec : ExecutionContext,
                        )
  extends MacroLogsDyn
{

  import esModel.api._

  maybeInit()

  /** Вся возня с ключами вертится здесь. */
  private def KF: KeyFactory = KeyFactoryFactory.newInstance()

  /** Пароль для секретного ключа сервиса. Можно добавить префикс пароля через конфиг. */
  private val SEC_KEY_PASSWORD: String = {
    val passwordRoot = """Y$mo[@QS-=S!A+W#ZMi;m]l9!,SNC]Ad_(9txd,?jb&"i{O#y'(\!)1yrTsI3m(@"""
    configuration.getOptional[String]("pgp.key.password.prefix")
      .fold (passwordRoot) { _ + passwordRoot }
  }

  private def getPw = SEC_KEY_PASSWORD.toCharArray

  /** Имя документа в модели ключей, где хранится непараноидальный ключ для защиты данных, хранимых в
    * browser.window.localStorage.
    * Изначально был только этот единствнный ключ в модели, и использовался для хранения пользовательских
    * данных в localStorage. */
  def LOCAL_STOR_KEY_ID = "lsk1"

  /** Генерация нового ключа защиты данных. */
  def genNewNormalKey(): MAsymKey = {
    val keyId = LOCAL_STOR_KEY_ID
    val key = KF.generateKeyPair(keyId, getPw)
    MAsymKey(
      pubKey = key.getPublicKey.toArmoredString,
      secKey = Some(key.toArmoredString),
      id     = Some(keyId)
    )
  }

  /** Запустить инициализацию, если задано в конфиге. */
  def maybeInit(): Option[Future[_]] = {
    val cfk = "pgp.keyring.init.enabled"
    if ( configuration.getOptional[Boolean](cfk).getOrElseFalse ) {
      Some(init())
    } else {
      mAsymKeys.getById(LOCAL_STOR_KEY_ID)
        // TODO проверять, что пароль соответствует ключу. Нужно пытаться зашифровать какие-то простые данные.
        .filter { _.isDefined }
        .failed
        .foreach {
          case _ : NoSuchElementException =>
            LOGGER.warn("PGP key does not exists and creation is disabled on this node: " + cfk)
          case ex =>
            LOGGER.error("Failed to check status of server's pgp key.", ex)
        }
      None
    }
  }

  /** Запустить инициализацию необходимых ключей. */
  def init(): Future[_] = {
    val fut = mAsymKeys.getById(LOCAL_STOR_KEY_ID)
      .filter { _.isDefined }
      .recoverWith { case _: NoSuchElementException =>
        val k = genNewNormalKey()
        mAsymKeys.save(k)
      }
    for (ex <- fut.failed) {
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

