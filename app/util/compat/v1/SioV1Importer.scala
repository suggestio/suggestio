package util.compat.v1

import com.ericsson.otp.erlang._
import play.api.Play.current
import io.suggest.util.{DateParseUtil, LogsImpl}
import models._
import scala.util.parsing.combinator._
import org.joda.time.DateTimeZone
import controllers.routes
import play.api.libs.concurrent.Execution.Implicits._
import java.util.UUID

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

    } finally {
      conn.close()
    }
  }


  /** Синхронный импорт старой блоготы в MBlog. Сохранение новых результатов в БД асинхронный. */
  private def importBlog(implicit conn: OtpConnection) {
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
      val recIter = recordTuple.elements().iterator

      // Первым элементом всегда идёт атом таблицы. Просто проверяем его для самоконтроля.
      ensureTableAtom(recIter.next(), blogTN)

      def nextStr = erlString2String(recIter.next())

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
      if (recIter.hasNext) {
        throw new IllegalArgumentException(logPrefix + "Cannot properly parse source record. There are unknown unparsed elements: " + recIter.mkString(", "))
      }

      val rec = MBlog(id=blogId, date=date, title=title, descr=desc, bgImage=bgImage, bgColor=bgColor, text=text)
      rec.save.onComplete {
        case scala.util.Success(_)  => trace(logPrefix + "Saved ok: " + blogId)
        case scala.util.Failure(ex) => error(logPrefix + "Failed to write record into DB: " + rec, ex)
      }
    }
  }


  /** Импорт данных из таблицы domain_data. В новой версии поле JSON вынесено в отдельную модель. */
  private def importDomainData(implicit conn: OtpConnection) {
    val ddTN = "domain_data"
    val logPrefix = "importDomainData(): "
    // [Id, Json, ShowImages, ShowContentText, ShowTitle, Renderer, DateCorrectionDays]
    // {domain_data, <<"pobeda.n-varshavka.omskedu.ru">>,
    //   <<"{\"lang\":\"ru\",\"search_field\":\"crnr-2\",\"search_layout\":\"t_style\",\"t_style_template\":\"sio-kk\",\"colors\":"...>>,
    //   false, true, true, 2, 0}
    dumpTable(ddTN).foreach { row =>
      val resIter = row.elements.iterator
      ensureTableAtom(resIter.next(), ddTN)

      // Далее, идут dkey и json в виде бинарей.
      def nextBinStr = new String(resIter.next().asInstanceOf[OtpErlangBinary].binaryValue)
      val dkey = nextBinStr
      val logPrefix1 = logPrefix + s"[$dkey] "
      val json = nextBinStr
      MDomainUserJson(dkey=dkey, data=json).save.onSuccess {
        case scala.util.Success(_)  => trace(logPrefix1 + "json successfully saved")
        case scala.util.Failure(ex) => error(logPrefix1 + "Failed to save domain json", ex)
      }

      // Считываем настройки отображения: ShowImages, ShowContentText, ShowTitle
      def nextBool = resIter.next().asInstanceOf[OtpErlangBoolean].booleanValue()
      val showImages = nextBool
      val showContentText = nextBool
      val showTitle = nextBool

      def nextInt = resIter.next().asInstanceOf[OtpErlangLong].intValue()
      val renderer = nextInt
      val withDateFiltering = nextInt

      if (resIter.hasNext) {
        throw new IllegalArgumentException(logPrefix1 + "Unknown input format. Unexpectedly retained elements: " + resIter.mkString(", "))
      }

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


  private def importPersonDomainAuthz(implicit conn: OtpConnection) {
    // [Id, PersonId, DomainId::binary(), BodyCode::binary(), CreatedUtc::datetime(), LastCheckedUtc::datetime(), VerifyInfo]
    // {person_domain, <<"p8IXimU8">>, <<"oink-oink@mail.ru">>, <<"tdprofel.ru">>,<<"sJTfgFuw2Ft7weuS">>,
    //                {{2013,6,10},{7,21,32}},  {{2013,12,12},{11,27,32}},  true}
    // {person_domain, <<27,36,121,208,222,21,76,61,157,49,135,98,67,114,61,179>>, <<"contact@qbba.com">>,<<"qbba.com">>,
    //             <<"siL6cRQ0L2wj7gn6">>,  {{2012,12,28},{17,57,17}},  {{2013,10,13},{18,2,57}},  {false, <<"not checked">>}}
    val pdTN = "person_domain"
    val logPrefix = "importPersonDomainAuthz(): "
    dumpTable(pdTN) foreach { row =>
      val rowIter = row.elements().iterator
      ensureTableAtom(rowIter.next(), pdTN)

      val idRaw = rowIter.next().asInstanceOf[OtpErlangBinary].binaryValue()
      val id = if (idRaw.length == 16) {
        // Это uuid в виде байт. Конвертим в строку
        importUuidBytesPure(idRaw)
      } else {
        // Тут голый qi id'шник. Просто извлечь ASCII строку из бинаря.
        new String(idRaw)
      }


      ???

      //val logPrefix1 = logPrefix + s"[$dkey] "
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
    conn.sendRPC(RPC_MODULE, RPCF_DUMP_TABLE, new OtpErlangList(new OtpErlangAtom(tableName)))
    val rpcResultTuple = conn.receiveRPC().asInstanceOf[OtpErlangTuple]
    if (!isTableRpcReply(RPCF_DUMP_TABLE, tableName, rpcResultTuple)) {
      throw new IllegalArgumentException("Unknown response from remote node: " + rpcResultTuple)
    }
    rpcResultTuple
      .elementAt(2).asInstanceOf[OtpErlangList]
      .elements().asInstanceOf[Array[OtpErlangTuple]]
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


  /** Приведение бинарных uuid эрланга к строкам средствами голой джавы. */
  private def importUuidBytesPure(b: Array[Byte]): String = {
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

}
