package io.suggest.sec

import javax.inject.Inject
import io.suggest.session.MSessionKeys._
import io.suggest.session.{LoginTimestamp, MSessionKeys}
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.UnivEqUtilJvm._
import io.suggest.util.logs.MacroLogsImpl
import play.api.mvc._
import play.api.mvc.request.{Cell, RequestAttrKey}

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.06.14 17:57
  * Description: Функции для сброса сессии при наступлении таймаута, и соотв.утиль для пролонгации сессии.
  *
  * 2014.feb.06: Из-за добавления в сессию securesocial, csrf-token и т.д. нужно аккуратнее работать с сессией,
  * без использования withNewSession().
  *
  * 2017.feb.28: Был трейт. Но он был довольно неудобен, т.к. вызывал необходимость создания лишних трейтов
  * и наследований в ActionBuilder'ах. После рефакторинга получилась класс с утилью, глобальный play-фильтр
  * и локальный Action, делающий тоже самое на уровне отдельных экшенов.
  *
  * Можно использовать так:
  * - Прописать один глобальный play-фильтр [[ExpireSessionFilter]] и забыть про эту подсистему.
  * - Без фильтра: везде втыкать action, как было раньше с трейтом.
  */


/** Утиль для v2 API обработки сессий в состоянии. */
final class ExpireSessionUtil @Inject() (
                                          implicit private val ec    : ExecutionContext
                                        )
  extends MacroLogsImpl
{

  /** Не обновлять timestamp сессии, если прошлый выставлен не позднее указанного кол-ва секунд
    * относительно текущего момента. */
  def GUESS_SESSION_AS_FRESH_SECONDS = 30

  /** Ключи в session, которые нужно удалять при любой проблеме с TTL. */
  def CLEAR_SESSION_FILTER_KEYS: Seq[String] = {
    MSessionKeys
      .onlyLoginIter
      .map(_.value)
      .toSeq
  }


  /** Подготовить сессию в исходном реквесте, собрав обновлённый реквест.
    *
    * @param rh0 Заголовки исходного реквеста.
    * @return Завёрнутый request header, O(1).
    */
  def prepareRequestHeader(rh0: RequestHeader): RequestHeader =
    _prepareReq( rh0 )

  def prepareRequest[A](request0: Request[A]): Request[A] =
    _prepareReq( request0 )


  /** Общий код prepare-части обработки исходного HTTP-запроса.
    *
    * @param rh0 Исходный реквест.
    * @tparam T Тип реквеста.
    * @return Обновлённый или исходный реквест.
    */
  private def _prepareReq[T <: RequestHeader](rh0: T): T = {
    // Подменяем исходный RequestHeader.session с помощью Request wrapper'а.
    val session0 = rh0.session
    val session2 = prepareRequestSession( session0 )
    if (session0 ===* session2) {
      rh0.addAttr(RequestAttrKey.Session, Cell(session2))
        .asInstanceOf[T]
    } else {
      rh0
    }
  }


  /** Анализ сессии и чистка от старья.
    *
    * @param session0 Исходная сессия.
    * @return Почищенная или исходная сессия.
    */
  private def prepareRequestSession(session0: Session): Session = {
    def logPrefix = s"prepareRequestSession(${session0.hashCode()})[${System.currentTimeMillis()}]:"

    val tstampOpt = LoginTimestamp.fromSession(session0)

    if (tstampOpt.isEmpty) {
      // Нет никакого timestamp'а в сессии. Убедиться, что в передаваемой в экшен сессии нет login-данных.
      // С учётом малых объемов данных, это быстрая read-only проверка:
      val clearSessionKeys = CLEAR_SESSION_FILTER_KEYS
      if ( session0.data.keysIterator.exists( clearSessionKeys.contains ) ) {
        // Внезапно, в сессии без TTL обнаружились какие-то важные данные. Пока просто ругаться в логи.
        val session2 = session0 -- clearSessionKeys
        LOGGER.error(s"$logPrefix Session without TTL contains login data! Re-cleared session data:\n old session = ${session0.data}\n new session = $session2")
        session2
      } else {
        // Ничего страшного внутри сессии не обнаружено. Просто возвращаем исходную сессию:
        session0
      }

    } else {
      // Если в сессии есть истёкший TTL, то нужно вернуть почищенную сессию.
      // Есть timestamp сессии. Разобраться, что с ним надо делать...
      val hasValidTs = tstampOpt.exists { ts =>
        val currTstamp = LoginTimestamp.currentTstamp()
        // Отфильтровать устаревшие timestamp'ы.
        ts.isTimestampValid(currTstamp)
      }

      if (hasValidTs) {
        // Таймштамп актуален и сейчас. Просто вернуть исходную сессию:
        session0
      } else {
        // Таймштамп истёк -- стереть из сессии таймштамп и username, вернуть обновлённую сессию.
        LOGGER.trace(s"$logPrefix Clearing expired session for person ${session0.get(PersonId.value)}")
        session0 -- CLEAR_SESSION_FILTER_KEYS
      }
    }
  }


  /** Статическая обработка результата работы экшенов на предмет сессии.
    *
    * @param request HTTP-реквест.
    * @param result Результат выполненного экшена.
    * @return Результат работы с поправленной сессией.
    */
  def processResult(request: RequestHeader, result: Result): Result = {
    val session0 = result.session(request)
    LoginTimestamp
      .fromSession(session0)
      .fold {
        if (session0.data contains PersonId.value) {
          // Сессия была только что выставлена в контроллере. Там же и ttl выставлен.
          val session1 = session0 + (Timestamp.value -> LoginTimestamp.currentTstamp().toString)
          result.withSession(session1)
        } else {
          // Не заниматься ковырянием сессии, если юзер не залогинен.
          result
        }

      } { ts =>
        // Уже есть таймштамп, значит пора залить новый (текущий) таймштамп в сессию.
        // Не требуется пересобирать сессию, если она недостаточно устарела. Это снизит нагрузку на сервер при использовании session-фильтров.
        val currTstamp = LoginTimestamp.currentTstamp()
        if ( Math.abs(currTstamp - ts.tstamp) <= GUESS_SESSION_AS_FRESH_SECONDS ) {
          // Не трогаем сессию. Она ещё недостаточно устарела.
          result
        } else {
          // Выставить новый timestamp, пересобрав/переподписав сессию:
          var sessionKeys = ts.withTstamp(currTstamp).toSessionKeys
          val session1 = session0 ++ sessionKeys
          result.withSession(session1)
        }
      }
  }

}


/** Экшен, который можно использовать для заворачивания реквеста и резульата экшена.
  * Не используется с момента создания, реализован быстренько и на всякий случай.
  * Если так и не будет использоваться, то можно будет его удалить.
  */
final class ExpireSessionAction @Inject() (
                                            defaultActionBuilder       : DefaultActionBuilder,
                                            expireSessionUtil          : ExpireSessionUtil,
                                            implicit private val ec    : ExecutionContext,
                                          ) {

  def apply[A](action: Action[A]): Action[A] = {
    defaultActionBuilder.async( action.parser ) { request0 =>
      val request2 = expireSessionUtil.prepareRequest( request0 )
      for (result <- action(request2)) yield
        expireSessionUtil.processResult(request2, result)
    }
  }

}


/** Глобальный фильтр для запросов и ответов на тему работы с TTL sio-сессияй. */
final class ExpireSessionFilter @Inject() (
                                            expireSessionUtil          : ExpireSessionUtil,
                                            implicit private val ec    : ExecutionContext
                                          )
  extends EssentialFilter
{

  override def apply(next: EssentialAction): EssentialAction = {
    EssentialAction { rh0 =>
      val rh2 = expireSessionUtil.prepareRequestHeader( rh0 )
      for (resp0 <- next(rh2)) yield
        expireSessionUtil.processResult(rh2, resp0)
    }
  }

}

