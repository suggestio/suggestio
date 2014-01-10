package util.compat.v1

import com.ericsson.otp.erlang._
import play.api.Play.current
import io.suggest.util.{DateParseUtil, LogsImpl}
import models._
import scala.util.parsing.combinator._
import org.joda.time.{DateTime, DateTimeZone}
import controllers.routes
import play.api.libs.concurrent.Execution.Implicits._
import java.util.UUID
import util.{QiCheckException, DomainQi}
import io.suggest.sax.SioJsV1

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.01.14 10:07
 * Description: Импорт данных из прошлого (v1) suggest.io, который был написан на эрланге.
 * Нужно импортировать пользователей со всеми ихними метаданными, данные по доменам и т.д.
 * Затем, надо бы импортировать домены, избавившись от уже deleted-доменов.
 *
 * Данные v1 хранятся в mnesia, поэтому нужен рабочий кластер sio-v1, и связь с ним.
 * Т.к. jinterface умеет ограничено работать с erlang-кластером, надо использовать rpc-библиотеку
 * на стороне старого sioweb v1, и клиент тут, который делать запросы к библиотеке, и обрабатывает ответы от него.
 * export-сервер просто дампит mnesia-таблицы и отправляет их в ответ клиенту (сюда).
 * Клиент парсит выхлоп и маппит оные на models.*
 *
 * Этот модуль вместе с jinterface-зависимостью надо бы удалить после завершения старта v2.
 */

object SioV1Importer extends JavaTokenParsers {

  private val LOGGER = new LogsImpl(getClass)
  import LOGGER._

  val LOCAL_NODE_NAME = current.configuration.getString("compat.v1.local.node.name") getOrElse "sioweb21"
  val REMOTE_SIOWEB_NODE_NAME = current.configuration.getString("compat.v1.remote.node.name") getOrElse "sioweb"

  // erlang-модуль на удалённой ноде, который содержит все необходимые функции.
  val RPC_MODULE = current.configuration.getString("compat.v1.remote.module.name") getOrElse "sioweb_v2rpc"

  // API предоставляемое RPC-модулем статично.
  val RPCF_DUMP_TABLE = "dump_table"
  val RPCF_ALL_KEYS   = "all_keys"

  val RPC_REPLY_TUPLE_ARITY = 3

  /** Удалять qi старее n дней. */
  private val QI_MAX_AGE_DAYS = current.configuration.getInt("compat.v1.qi.age.max.days") getOrElse 10

  private val TZ_DFLT = DateTimeZone.forID("Europe/Moscow")

  // Формат RPC-ответов фунцкий: {RPCF:String, args:OtpErlangList[_], Result}

  def doImportSync() {
    val logPrefix = "doImportSync(): "
    trace(logPrefix + "Starting...")
    val selfNode = new OtpSelf(LOCAL_NODE_NAME)
    val remoteSiowebNode = new OtpPeer(REMOTE_SIOWEB_NODE_NAME)
    implicit val conn = selfNode connect remoteSiowebNode
    trace(logPrefix + s"Connected self:$LOCAL_NODE_NAME -> remote:$REMOTE_SIOWEB_NODE_NAME. conn=$conn")

    try {
      // Можно начинать импорт
      importBlog
      importDomainData
      importPersonDomainAuthz
      importDomainQi
      // Можно не импортировать юзеров. Полезных данных в person нет всё равно, а залогиненные юзеры будут созданы автоматом.
      //importPerson
      // В финале, заливаем сайты. Хотя это можно сделать и в начале.
      importDomains

    } finally {
      conn.close()
    }
  }


  /** Синхронный импорт старой блоготы в MBlog. Сохранение новых результатов в БД асинхронный. */
  def importBlog(implicit conn: OtpConnection) {
    val logPrefix = "importBlog(): "
    // Импортируем таблицу записей blog_record, которая имеет следующий формат.
    // [Id, Date, Title, Description, BgImage, BgColor, Text]
    // Id = "blog_record-1"
    // Date = [49,48,32,208,190,208,186,...] = "10 декабря 2012" utf8
    // Title = Description = Text = [83,117,103,103,101,115,116,46,...] = "Suggest.io..." utf8
    // BgImage = "/static/images2/1_cover_big.png"
    // BgColor = "000000"
    val blogTN = "blog_record"

    // Есть ожидаемый ответ. Пора распарсить результаты и залить их в MBlog.
    val blogIdParser = activeRecIdStrParser(blogTN)
    dumpTable(blogTN).foreach { recordTuple =>
      // Проход по элементам таблицы.
      val rowIter = recordTuple.elements().iterator

      // Первым элементом всегда идёт атом таблицы. Просто проверяем его для самоконтроля.
      ensureTableAtom(rowIter.next(), blogTN)

      def nextStr = erlString2String(rowIter.next())

      // Таблица выверена. Далее, извлекаем id, отбрасывая богомерзкий ActiveRecord-префикс.
      val arecBlogId = nextStr
      val blogId = parseAll(blogIdParser, arecBlogId).get

      // Пора прочухать дату, которая идёт вслед за id. Используем парсер дат, т.к. она в исходнике в убогом формате.
      val dateAsStr = nextStr
      val date = DateParseUtil.extractDates(dateAsStr).head.toDateMidnight(TZ_DFLT).toDateTime(TZ_DFLT)

      // Далее идёт заголовок записи (title) и description
      val title = nextStr
      val desc  = nextStr

      // bg_image - ссылка на фоновую картинку. Следует сразу подменять старую адресацию на новую.
      val bgImagePathOld = nextStr
      val bgImageFilename = bgImagePathOld.replaceFirst("^/static/images\\d*", "images")
      val bgImage = routes.Assets.at(bgImageFilename).url

      // BgColor - строка, кодирующая фон
      val bgColor = nextStr

      // Text - писанина какая-то
      val text = nextStr

      // По идее, всё. Но если остались ещё элементы, то это означает, что запись прочиталась неправильно.
      ensureEmptyIter(rowIter)

      val rec = MBlog(id=blogId, date=date, title=title, descr=desc, bgImage=bgImage, bgColor=bgColor, text=text)
      rec.save.onComplete {
        case scala.util.Success(_)  => trace(logPrefix + "Saved ok: " + blogId)
        case scala.util.Failure(ex) => error(logPrefix + "Failed to write record into DB: " + rec, ex)
      }
    }
  }


  /** Импорт данных из таблицы domain_data. В новой версии поле JSON вынесено в отдельную модель. */
  def importDomainData(implicit conn: OtpConnection) {
    // [Id, Json, ShowImages, ShowContentText, ShowTitle, Renderer, DateCorrectionDays]
    // {domain_data, <<"pobeda.n-varshavka.omskedu.ru">>,
    //   <<"{\"lang\":\"ru\",\"search_field\":\"crnr-2\",\"search_layout\":\"t_style\",\"t_style_template\":\"sio-kk\",\"colors\":"...>>,
    //   false, true, true, 2, 0}
    val ddTN = "domain_data"
    val logPrefix = "importDomainData(): "
    trace(logPrefix + "Starting...")
    dumpTable(ddTN).foreach { row =>
      val rowIter = row.elements.iterator
      ensureTableAtom(rowIter.next(), ddTN)

      // Далее, идут dkey и json в виде бинарей.
      def nextBinStr = new String(rowIter.next().asInstanceOf[OtpErlangBinary].binaryValue)
      val dkey = nextBinStr
      val logPrefix1 = logPrefix + s"[$dkey] "
      val json = nextBinStr
      MDomainUserJson(dkey=dkey, data=json).save.onSuccess {
        case scala.util.Success(_)  => trace(logPrefix1 + "json successfully saved")
        case scala.util.Failure(ex) => error(logPrefix1 + "Failed to save domain json", ex)
      }

      // Считываем настройки отображения: ShowImages, ShowContentText, ShowTitle
      def nextBool = rowIter.next().asInstanceOf[OtpErlangBoolean].booleanValue()
      val showImages = nextBool
      val showContentText = nextBool
      val showTitle = nextBool

      def nextInt = rowIter.next().asInstanceOf[OtpErlangLong].intValue()
      val renderer = nextInt
      val withDateFiltering = nextInt

      ensureEmptyIter(rowIter)

      MDomainUserSettings.getForDkey(dkey) flatMap { settings =>
        import util.domain_user_settings.DUS_Basic._
        val newSettings: MDomainUserSettings.DataMap_t = Map(
          KEY_RENDERER    -> renderer,
          KEY_SHOW_CONTENT_TEXT -> showContentText,
          KEY_SHOW_TITLE  -> showTitle,
          KEY_SHOW_IMAGES -> showImages
        )
        settings.withData(newSettings).save
      } onComplete {
        case scala.util.Success(_)  => trace(logPrefix1 + "Domain settings successfully saved")
        case scala.util.Failure(ex) => error(logPrefix1 + "Failed to save domain settings", ex)
      }
    }
  }


  def importPersonDomainAuthz(implicit conn: OtpConnection) {
    // [Id, PersonId, DomainId::binary(), BodyCode::binary(), CreatedUtc::datetime(), LastCheckedUtc::datetime(), VerifyInfo]
    // {person_domain, <<"p8IXimU8">>, <<"oink-oink@mail.ru">>, <<"tdprofel.ru">>,<<"sJTfgFuw2Ft7weuS">>,
    //                {{2013,6,10},{7,21,32}},  {{2013,12,12},{11,27,32}},  true}
    // {person_domain, <<27,36,121,208,222,21,76,61,157,49,135,98,67,114,61,179>>, <<"contact@qbba.com">>,<<"qbba.com">>,
    //             <<"siL6cRQ0L2wj7gn6">>,  {{2012,12,28},{17,57,17}},  {{2013,10,13},{18,2,57}},  {false, <<"not checked">>}}
    val pdTN = "person_domain"
    val logPrefix = "importPersonDomainAuthz(): "
    trace(logPrefix + "Starting...")
    dumpTable(pdTN) foreach { row =>
      val rowIter = row.elements().iterator
      ensureTableAtom(rowIter.next(), pdTN)

      def nextBin = rowIter.next().asInstanceOf[OtpErlangBinary].binaryValue()

      val idRaw = nextBin
      val (id, typ): (String, String) = if (idRaw.length == 16) {
        // Это uuid в виде байт. Конвертим в строку
        importUuidBytesPure(idRaw) -> MPersonDomainAuthz.TYPE_VALIDATION
      } else {
        // Тут голый qi id'шник. Просто извлечь ASCII строку из бинаря.
        new String(idRaw) -> MPersonDomainAuthz.TYPE_QI
      }

      def nextStr = new String(nextBin)
      val personId = nextStr
      val dkey = nextStr
      val bodyCode = nextStr

      // Пора пропарсить erl-даты. Они в TZ MSK
      val dtParser = erl2jodaDt(DateTimeZone.UTC)
      def nextDt = dtParser(rowIter.next())
      val createdUtc = nextDt
      val lastCheckedUtc = nextDt

      // Пропарсить VerifyInfo = true | {false, Msg::binary()}
      val (isVerified, lastErrors): (Boolean, List[String]) = rowIter.next() match {
        case errTuple: OtpErlangTuple if errTuple.arity() == 2 =>
          val bool = errTuple.elementAt(0).asInstanceOf[OtpErlangBoolean].booleanValue()
          val errMsg = new String(errTuple.elementAt(1).asInstanceOf[OtpErlangBinary].binaryValue())
          bool -> List(errMsg)

        case erlTrue: OtpErlangBoolean =>
          erlTrue.booleanValue() -> Nil
      }
    
      // Убедится, что данных в ряде больше нет
      ensureEmptyIter(rowIter)

      // Всё распарсено ок. Собрать запись и отправить её в хранилище.
      val authz = MPersonDomainAuthz(
        id=id, dkey=dkey, personId=personId, typ=typ, bodyCode=bodyCode, dateCreated=createdUtc,
        dtLastChecked=lastCheckedUtc, isVerified=isVerified, lastErrors=lastErrors
      )
      authz.save.onComplete {
        case scala.util.Success(_)  => trace(logPrefix + "Saved OK")
        case scala.util.Failure(ex) => error(logPrefix + "Cannot save " + authz, ex)
      }
    }
  }


  /** Импорт данных qi. Актуальный qi-данных может и не быть в момент импорта, однако всё равно надо это сделать,
    * чтобы юзеры, проводящие установку скрипта сайта, не ругались. Старые qi надо отфильтровать. */
  def importDomainQi(implicit conn: OtpConnection) {
    // [Id, DomainId, StartUrlBin, SessionIdBin, DateCreatedUtc]
    // {domain_qi,<<"SacjL3Pf">>,<<"drom.ru">>, <<"http://drom.ru/">>, <<"b9a08bf0265dfcd4ec86f34343ea69e096192202">>, {{2012,12,12},{13,33,48}} }
    val logPrefix = "importDomainQi(): "
    val dQiTN = "domain_qi"
    trace(logPrefix + "Starting...")
    val qiOldestDt = DateTime.now().minusDays(QI_MAX_AGE_DAYS)
    dumpTable(dQiTN) foreach { row =>
      val rowIter = row.elements().iterator
      ensureTableAtom(rowIter.next(), dQiTN)

      def nextStr = new String(rowIter.next().asInstanceOf[OtpErlangBinary].binaryValue())

      val qiId = nextStr
      val dkey = nextStr
      val startUrl = nextStr
      val bossSessionId = nextStr // unused
      val dateCreated = erl2jodaDt(DateTimeZone.UTC)(rowIter.next())

      ensureEmptyIter(rowIter)

      // Если qi староват, то его надо выкинуть.
      val logPrefix1 = logPrefix + s"[$dkey/$qiId] "
      if (dateCreated isAfter qiOldestDt) {
        val qiRec = MDomainQiAuthzTmp(
          dkey = dkey,
          id = qiId,
          dateCreated = dateCreated
        )
        qiRec.save.onComplete {
          case scala.util.Success(_)  => trace(logPrefix1 + "Qi saved ok")
          case scala.util.Failure(ex) => error(logPrefix1 + "Failed to save qi", ex)
        }
      } else {
        trace(logPrefix1 + s"NOT importing too old qi: $dkey/$qiId ;; creation date = $dateCreated")
      }
    }
  }


  /** Импорт таблицы person. Там ничего конкретного по юзерам так и не появилось. */
  private def importPerson(implicit conn: OtpConnection) {
    // [Id, Info], Второе поле вроде всегда -- пустой список. Создано было, т.к. ChicagoBoss глючил от модели без полей-значений.
    // {person, <<"dmitriy2512@gmail.com">>, []}
    val personTN = "person"
    val logPrefix = "importPerson(): "
    trace(logPrefix + "Starting...")
    dumpTable(personTN) foreach { row =>
      val rowIter = row.elements().iterator
      ensureTableAtom(rowIter.next(), personTN)

      val personId = new String(rowIter.next().asInstanceOf[OtpErlangBinary].binaryValue())
      rowIter.next()  // Сброс поля Info

      ensureEmptyIter(rowIter)

      val rec = MPerson(id = personId, lang = "en")
      rec.save.onComplete {
        case scala.util.Success(_)  => trace(logPrefix + "Person saved OK: " + personId)
        case scala.util.Failure(ex) => error(logPrefix + "Failed to save person " + personId, ex)
      }
    }
  }


  /**
   * Кульминация: импорт всех доменов. Точнее, не всех, а только реально необходимых.
   *
   * Используем таблицу js_install для определения сайтов с установленными доменами. Если какой-либо сайт висит в
   * removed (бывает, когда suggest.io установлен только на части сайта), то сайт будет установлен при реквесте js'а.
   *
   * Фильтруем паразитные домены по регэкспам: прошлый suggest.io не проверял dkey, указанный в qi-js-части, что
   * давало возможность устанавливать левые домены (в основном - по ошибкам в DNS). Нужно тут отработать скрипты sio v1,
   * когда скрипт был для всех одинаковым. checkQiAsync возвращает в таких случаях failed Future, содержащий в
   * поле [[util.QiCheckException.jsFound]] непустой список со скриптом v1.
   */
  def importDomains(implicit conn: OtpConnection) {
    // domain_js_install: [Dkey::string, FirstSeenUtcTs::long, LastSeenUtcTs::long, IsInstalledNow::bool]
    // {domain_js_install, <<"offtop.ru">>, 63537639263, 63537708570, false}
    // Оптимизация: Есть кучка сайтов с кучей паразитных поддоменов. Чтобы не тратить время на их перепроверку, резать расово по маске.
    val stripSubdomains = Set[String](
      "nadonenado.ru",
      "zautoshop.com",
      "mariabuchman.com",
      "ldpr-tube.ru",
      "ldpr.ru",
      "ipkro-38.ru"
    )
    val logPrefix = "importDomains(): "
    val dTN = "domain_js_install"
    dumpTable(dTN) foreach { row =>
      val rowIter = row.elements().iterator
      ensureTableAtom(rowIter.next(), dTN)

      // Ключ ряда - это dkey
      var dkey = new String(rowIter.next().asInstanceOf[OtpErlangBinary].binaryValue())
      var logPrefix1 = logPrefix + s"[$dkey] "
      // timestamp'ы в неведомом формате дропаем
      rowIter.next()
      rowIter.next()

      val isInstalledNow = rowIter.next().asInstanceOf[OtpErlangBoolean].booleanValue()
    
      if (isInstalledNow) {
        // Выставлено, что домен установлен. Надо прогнать его через раннюю стрипалку поддоменов
        // и затем через изменённую процедуру перевалидации, т.к. дохлые сайты и некорекные домены тоже имеют такой флаг.
        val maybeNewDkey = stripSubdomains.find { upperDomain =>
          dkey endsWith ("." + upperDomain)
        }
        if (maybeNewDkey.isDefined) {
          dkey = maybeNewDkey.get
          logPrefix1 = logPrefix + s"[$dkey] "
        }

        // Стрип паразитных поддоменов сделан. Запускаем асинхронную проверку. Следует отрабатывать случай с sio.js v1.
        DomainQi.checkQiAsync(
          dkey    = dkey,
          url     = "http://" + dkey + "/",
          qiIdOpt = None,
          sendEvents = false,
          pwOpt   = None
        ) recover {
          // Если найден sio.js v1, то это тоже как бы валидно.
          case ex: QiCheckException if ex.jsFound.headOption.exists(_ == SioJsV1) =>
            DomainQi.validSioJsFoundOn(dkey, pwOpt=None)
            null
        } onComplete {
          // В зависимости от результатов работы, вывести то или иное сообщение.
          case scala.util.Success(maybeJsV2) =>
            val jsMsg = if (maybeJsV2 != null) maybeJsV2.toString else "SioJsV1"
            info(logPrefix1 + jsMsg + " found")

          case scala.util.Failure(ex) =>
            warn(logPrefix1 + "domain check sio.js failed: " + ex.getClass.getSimpleName + " :: " + ex.getMessage)
        }

      } else {
        // Игнорим домен, у которого выставлен isInstalledNow = false. Если флаг выставлен по ошибке, то домен будет
        // добавлен автоматически позже, а не сейчас.
        trace(logPrefix1 + "isInstalledNow = false;; skipping import")
      }
    }
  }


  /** Проверка возвращаемого RPC-ответа, т.к. сам менеджер соединений jinterface этим делом не занимается. */
  private def isTableRpcReply(rpcf: String, tableName: String, rpcResultTuple: OtpErlangTuple): Boolean = {
    rpcResultTuple.arity() == RPC_REPLY_TUPLE_ARITY && {
      val isRpc = rpcResultTuple.elementAt(0) match {
        case a: OtpErlangAtom => a.atomValue == rpcf
        case _ => false
      }
      isRpc && {
        rpcResultTuple.elementAt(1) match {
          case l: OtpErlangList => l.getHead match {
            case a2: OtpErlangAtom => a2.atomValue() == tableName
            case _ => false
          }

          case _ => false
        }
      }
    }
  }


  /** Парсер строк на основе базовых erl-объектов, используемых для строкового представления. */
  private val erlString2String: PartialFunction[OtpErlangObject, String] = {
    case s: OtpErlangString =>
      s.stringValue

    case bytesList: OtpErlangList =>
      // TODO Может быть bytesList, а может быть и utf codepoints list.
      val bytes = bytesList.elements().map {
        case b: OtpErlangLong => b.byteValue()
      }
      new String(bytes)

    case binary: OtpErlangBitstr =>
      new String(binary.binaryValue)
  }


  private def dumpTable(tableName: String)(implicit conn: OtpConnection): Array[OtpErlangTuple] = {
    val logPrefix = s"dumpTable($tableName): "
    trace(logPrefix + "Starting...")
    val startedAt = System.currentTimeMillis
    conn.sendRPC(RPC_MODULE, RPCF_DUMP_TABLE, new OtpErlangList(new OtpErlangAtom(tableName)))
    val rpcResultTuple = conn.receiveRPC().asInstanceOf[OtpErlangTuple]
    if (!isTableRpcReply(RPCF_DUMP_TABLE, tableName, rpcResultTuple)) {
      throw new IllegalArgumentException("Unknown response from remote node: " + rpcResultTuple)
    }
    val result = rpcResultTuple
      .elementAt(2).asInstanceOf[OtpErlangList]
      .elements().asInstanceOf[Array[OtpErlangTuple]]
    trace {
      val finishedAt = System.currentTimeMillis()
      val tookMs = finishedAt - startedAt
      s"Dump done. took = $tookMs ms, ${result.length} rows"
    }
    result
  }


  private val INT_REGEX: Parser[String] = "\\d+".r


  /** Сборка парсера для ActiveRecord id. Выходной id целочисленный, но в виде строки. */
  private def activeRecIdStrParser(tableName: String): Parser[String] = {
    tableName ~> "-" ~> INT_REGEX
  }

  /** activeRecord содержит целочисленные id в хвосте. Поэтому тут парсер, выводящий числа вместо чисел в виде строк. */
  private def activeRecIdParser(tableName: String): Parser[Int] = {
    activeRecIdStrParser(tableName) ^^ { _.toInt }
  }


  /** Убедится, что указанный erl-объект является атомом имени указанной таблицы. */
  private def ensureTableAtom(e: OtpErlangObject, tableName: String) {
    val isOk = e match {
      case a: OtpErlangAtom =>
        a.atomValue() == tableName

      case _ => false
    }
    if (!isOk) {
      throw new IllegalArgumentException(s"Table name atom $tableName expected, but curr row from other table or unknown format: " + e)
    }
  }


  /** Приведение бинарных uuid эрланга к строкам средствами голой джавы. Это быстро и скорее всего работает как надо. */
  private def importUuidBytesPure(b: Array[Byte]): String = {
    // Код взят из почему-то-private конструктора UUID.
    var msb = 0L
    var lsb = 0L
    for (i <- 0 until 8) {
      msb = (msb << 8) | (b(i) & 0xff)
    }
    for (i <- 8 until 16) {
      lsb = (lsb << 8) | (b(i) & 0xff)
    }
    new UUID(msb, lsb).toString
  }


  /** Приведение бинаря с байтами uuid из эрланга к строке через RPC-вызов ossp import. Можно использовать как fallback. */
  private def importUuidBytesErl(oeb: OtpErlangBinary)(implicit conn: OtpConnection): String = {
    val textAtom = new OtpErlangAtom("text")
    val args = new OtpErlangList(Array(oeb, textAtom))
    conn.sendRPC("ossp_uuid", "import", args)
    conn.receiveRPC() match {
      case tb: OtpErlangBinary => new String(tb.binaryValue())
    }
  }


  /** Перевести дату в erl-формате {{2013,6,10},{7,21,32}} в нормальное представление. */
  private def erl2jodaDt(tz: DateTimeZone): PartialFunction[OtpErlangObject, DateTime] = {
    case dtTuple: OtpErlangTuple if dtTuple.arity() == 2 =>
      val Array(d, t) = dtTuple.elements().asInstanceOf[Array[OtpErlangTuple]]
      def erlInt2int: PartialFunction[OtpErlangObject, Int] = {
        case l: OtpErlangLong => l.intValue()
      }
      val Array(year, month, day) = d.elements().map(erlInt2int)
      val Array(hour, min, sec) = t.elements().map(erlInt2int)
      new DateTime(year, month, day, hour, min, sec, tz)
  }
  
  
  /** После прохода ряда следует убеждаться, что итератор элементов ряда не имеет больше данных. */
  private def ensureEmptyIter(rowIter: Iterator[_]) = {
    if (rowIter.hasNext) {
      throw new IllegalArgumentException("Unknown input format. Unexpectedly retained elements: " + rowIter.mkString(", "))
    }
  }

}
