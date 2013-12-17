package controllers

import _root_.util.acl.{AbstractRequestWithPwOpt, MaybeAuth}
import play.api.mvc._
import play.api.data._, Forms._
import util.FormUtil._
import io.suggest.util.UrlUtil
import util._
import play.api.libs.json._
import play.api.libs.Jsonp
import play.api.libs.concurrent.Execution.Implicits._
import scala.Some
import models.MDomain
import play.api.Play.{current, configuration}
import gnu.inet.encoding.IDNA
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.13 16:13
 * Description: Контроллер выполнения поисковых запросов. Получает запрос и возвращает поисковую выдачу.
 */

object Search extends SioController with Logs {

  import LOGGER._

  /** Макс кол-во тегов, которое пользователь может указать к одному запросу. */
  val MAX_TAGS_PER_SEARCH = configuration.getInt("search.facet.invlink.request.tags.max") getOrElse 5

  /** Сколько максимум доменов может передать пользователь. */
  val MAX_DOMAINS_PER_SITE_SEARCH = configuration.getInt("search.site.max_domains") getOrElse 5

  val DOMAIN_LIST_SEP = ","
  val LANG_LIST_SEP = ","


  /** Маппинг для поисковых запросов по сайтам.
    * TODO Для глобального поискового маппинга надо вынести мапперы отсюда в отдельные костанты класса и собрать ещё один маппинг.
    *
    * Когда станет больше 18 параметров или просто пора будет разбивать, то нужно будет глянуть QueryStringBindable,
    * которая поддерживается на уровне роутера и может маппить группы ключей на произвольные объекты.
    * Или генерить новые маппинги вида:
    * mapping[R, A1, ... A19, A20, ...](a1:A1, ..., a19:A19, ...)
    * или разбивать маппинг на несколько других. */
  val siteSearchRequestFormM = Form(mapping(
    /* Базовые составляющие поискового запроса для сайта */

    // Поисковый запрос. Вся суть тут.
    "q" -> nonEmptyText(maxLength = 127)
      .transform(strTrimLowerF, strIdentityF)
      .verifying("c.s.search_query.empty", !_.isEmpty)
      // TODO Следует научится обнаруживать параметры прямо в поисковой строке. например "site:vasya.com скачать бесплатно без смс"
    ,

    // Домены, в которых ищем. Если несколько доменов, то они могут быть склеены через DOMAIN_LIST_SEP. Если не задано,
    // то глобальный поиск. Результат маппинга сразу нормализуется к списку уникальных dkey.
    "h" -> {
      // Токенизировать и нормализовать хостнеймы до dkey
      nonEmptyText(maxLength = 99)
        .transform(
          { _.split(DOMAIN_LIST_SEP)
            .foldLeft[List[String]] (Nil) { (acc, domain) =>
              // Нужно как бы применить dkey-маппинги на каждый из доменов. Т.к. сделать это напрямую как-то нельзя, делаем нормализацию руками
              try {
                UrlUtil.host2dkey(domain) :: acc
              } catch { case ex: Exception =>
                if (isTraceEnabled) {
                  info(s"Suppressed ${ex.getClass.getSimpleName} while parsing domain string: $domain", ex)
                } else {
                  info(s"Suppressed ${ex.getClass.getSimpleName} while parsing domain string: $domain :: ${ex.getMessage}")
                }
                acc
              }
            }.reverse   // Восстановить исходный порядок после foldLeft()
          },

          // unapply: слепить строку доменов назад, денормализовав в юникод IDN-домены.
          {domains: List[String] => domains.map(IDNA.toUnicode).mkString(DOMAIN_LIST_SEP) }
        )
      // Если параметр domain встретился несколько раз, то надо его склеить и дедублицировать, убедившись, что он не слишком длинный.
      .transform(_.distinct, identity[List[String]])
      .verifying { dkeys =>
        val dkeyCount = dkeys.size
        dkeyCount > 0  &&  dkeyCount <= MAX_DOMAINS_PER_SITE_SEARCH
      }
    },

    /* Разные опциональные параметры сайтового поиска. */

    // TODO Не помню, локали тут или iso2/iso3 языков?
    "lang"  -> {
      val langValueM = nonEmptyText(2, 16)
        .transform(
          _.split(LANG_LIST_SEP).toList.distinct,
          {langs: List[String] => langs.mkString(LANG_LIST_SEP)}
        )
      optional(langValueM)
        .transform(optList2ListF, list2OptListF[String])
    }
    ,

    // Дебаг - выдавать доп инфу при поиске. Следует фильтровать до false, если юзер не админ.
    "debug" -> optional(boolean).transform(
      _ getOrElse false,
      {bool: Boolean => if (bool) Some(bool) else None}
    )
    ,

    // Фасеты: автоопределённые теги для страниц в sio.analysis.facet.invlink.
    // Используется list-маппер, поэтому ключ надо кодировать с указанием уникального индекса, т.е. fi.search.tags[222]=Хата%20ок&...
    "fi.search.tags" ->
      optional(
        list(nonEmptyText(maxLength = 40))
          .transform(
            {tags => if (tags.size > MAX_TAGS_PER_SEARCH)  tags.slice(0, MAX_TAGS_PER_SEARCH)  else  tags },  // TODO Возможно, стоит использовать list.nthtail()?
            identity[List[String]]
          )
      ).transform(optList2ListF, list2OptListF[String])

  )
  // apply-фунцкия. Преобразует кучу хлама в некое удобное для работы представление.
  {(queryStr, dkeys, langs, isDebug, fiInTags) =>
    new SioSearchOptions(
      queryStr = queryStr,
      dkeys = dkeys,
      langs = langs,
      isDebug = isDebug,
      fiSearhInTags = fiInTags
    )
  }
  // unapply
  {sso => Some(sso.queryStr, sso.dkeys, sso.langs, sso.isDebug, sso.fiSearhInTags)}
  )


  /** Привычный GET-запрос поиска. Используется, но с расширением поискового функционала, его стало не хватать.
    * Удалять его в будущем тоже нельзя, ибо при появлении глобального поиска, люди могут захотеть давать в инете ссылки на поиск. */
  def siteSearchGET = MaybeAuth.async { implicit request =>
    // bindFromRequest(qs) дропает значения повторяющихся ключей и берёт только первое ключ-значение. Т.е. если объявить
    // несколько значений с одинаковыми ключами, то все кроме первого будут проигнорированы.
    siteSearchRequestFormM.bindFromRequest(request.queryString).fold(
      {formWithErrors =>
        warn {
          import request._
          val logErrMsg = formWithErrors.errors.map { fe => fe.key + ": " + fe.message }.mkString(" ;; ")
          s"siteSearchGET(): $uri - not parsed search request: $logErrMsg user=$pwOpt from=$remoteAddress"
        }
        NotAcceptable(formWithErrors.errorsAsJson)
      }
      ,
      siteSearchAsync
    )
  }


  /* ========================================= Internal functions ================================================= */

  /**
   * Запрос на поиск по сайту (сайтам) приходит сюда.
   * @return Future[Jsonp] с результатом поиска или что-то, описывающее проблему.
   */
  private def siteSearchAsync(sso: SioSearchOptions)(implicit request: AbstractRequestWithPwOpt[AnyContent]): Future[SimpleResult] = {
    import sso.{dkeys, queryStr, isDebug, langs}

    lazy val logPrefix = s"siteSearch(${dkeys.mkString(",")}, q:$queryStr): "
    trace(s"$logPrefix debug=$isDebug langs=${langs.mkString(",")}")
    // Нужно перегнать все dkey в MDomain, чтобы отсеять все неустановленные домены, а затем принять решение о поиске.
    if (dkeys.isEmpty) {
      // Запрошен поиск по сайтам, но вместо доменов передана какая-то чехарда или вообще ничего не передано.
      debug(logPrefix + "No dkeys passed")
      domainsNotFound(dkeys)

    } else {
      MDomain.maybeGetSeveral(dkeys) flatMap {
        // Запрошен поиск по несуществующим доменам. Это ошибочная ситуация.
        case noDomains if noDomains.isEmpty =>
          debug(logPrefix + s"dkeys passed is ${dkeys.mkString(",")}, but they not found in MDomain.")
          domainsNotFound(dkeys)

        // Запрошен поиск по известным доменам.
        case mdomains =>
          val newDkeys = mdomains.map(_.dkey)
          trace(logPrefix + "some or all domains found: " + newDkeys.mkString(","))
          // Если не-админ выставил debug-флаг, то надо проигнорить debug
          if (sso.isDebug && !request.isSuperuser) {
            sso.isDebug = false
          }

          // TODO Надо бы как-то выявлять некорректные фасеты

          // TODO нужно восстанавливать SearchContext из кукисов реквеста или генерить новый
          // TODO Настройки поиска, заданные юзером, надо извлекать из модели DomainData
          sso.domains = mdomains
          sso.dkeys = newDkeys
          // Отправляем время выполнения поиска в логи, если включен trace'инг.
          val searchStartedAt: Long = if (isTraceEnabled) System.currentTimeMillis else -1L
          SiowebEsUtil.searchDkeys(sso) map { searchResult =>
            import searchResult.hits
            val currTime = System.currentTimeMillis
            trace {
              val tookMs = currTime - searchStartedAt
              s"${logPrefix}search completed with ${hits.size} results. [$tookMs ms]"
            }
            // Отрендерить результаты в json-е. Собираем карту (список) ключ-значение.
            var jsonEntries: List[(String, JsValue)] = Nil
            // Если есть статистика по фасетам, то надо их отобразить наверное.
            if (!searchResult.fiTags.isEmpty) {
              val fiJsMap = searchResult.fiTags.map { case (term, cnt)  =>  term -> JsNumber(cnt) }
              jsonEntries  ::=  "fiTags" -> JsObject(fiJsMap)
            }
            jsonEntries =
              "status"        -> JsString("ok") ::
              "timestamp"     -> JsNumber(currTime) ::
              "search_result" -> JsArray(hits.map(_.toJsValue)) ::
              jsonEntries

            val jsonp = Jsonp("sio._s_add_result", JsObject(jsonEntries))
            // TODO Сохранить обновлённый searchContext и серилизовать в кукис ответа
            Ok(jsonp)

          } recover { case ex =>
            error(logPrefix + "Search failed", ex)
            ex match {
              case _:NoSuchDomainException      => NotFound(ex.getMessage)
              case _:EmptySearchQueryException  => ExpectationFailed(ex.getMessage)
              case _                            => InternalServerError(ex.getMessage)
            }
          }
      }
    }
  }


  /** Вернуть текст, что не найдено доменов, в которых надо искать. */
  private def domainsNotFound(dkeys: Seq[String]) = {
    val domainsStr = dkeys.mkString(", ")
    // TODO нужно наверное запускать проверку на предмет наличия на этих сайтах установленных скриптов?
    NotFound("Domain not found: " + domainsStr)
  }

}
