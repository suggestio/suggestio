package util.ad

import javax.inject.{Inject, Singleton}
import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths}
import io.suggest.color.MColorData
import io.suggest.common.empty.OptionUtil
import io.suggest.err.ErrorConstants
import io.suggest.file.up.MFile4UpProps
import io.suggest.img.MImgFmts
import io.suggest.jd._
import io.suggest.jd.tags._
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.js.UploadConstants
import io.suggest.model.n2.edge.{EdgeUid_t, MPredicates}
import io.suggest.primo.id.IId
import io.suggest.scalaz.StringValidationNel
import io.suggest.text.StringUtil.StringCollUtil
import io.suggest.util.logs.MacroLogsImpl
import japgolly.univeq._
import models.mctx.Context

import scala.concurrent.Future
import scalaz.{Tree, Validation, ValidationNel}
import scalaz.syntax.apply._
import util.n2u.N2VldUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 10:15
 * Description: Общая утиль для работы с разными ad-формами: preview и обычными.
 */
@Singleton
class LkAdEdFormUtil @Inject() (
                                 n2VldUtil    : N2VldUtil
                               )
  extends MacroLogsImpl
{

  /** Макс.длина загружаемой картинки в байтах. */
  def IMG_UPLOAD_MAXLEN_BYTES: Int = {
    val mib = 40
    mib * 1024 * 1024
  }


  //---------------------------------------------------------------------------
  // v2 react form

  /** Срендерить и вернуть дефолтовый документ пустой карточки для текущего языка. */
  def defaultEmptyDocument(implicit ctx: Context): Future[MJdData] = {
    // TODO Брать готовую карточку из какого-то узла и пробегаться по эджам с использованием messages.

    // Тут просто очень временный документ.
    // Потом надо будет просто искать карточки на определённом узле, и возвращать их документ
    // и эджи (эджи - прорендерив через ctx.messages). Карточки сгенерить через новый редактор (который ещё не написан).

    val tplTree = Tree.Node(
      root = JdTag.document,
      forest = Stream(
        // Уровень стрипов. Рендерим три стрипа.

        // Strip#1 содержит намёк на то, что это верхний блок.
        Tree.Leaf {
          JdTag.block(
            bm = BlockMeta(
              w = BlockWidths.default,
              h = BlockHeights.default,
            ),
            bgColor = Some(MColorData(
              code = "060d45"
            ))
          )
        }

      )
    )

    val r = MJdData(
      doc = MJdDoc(
        template  = tplTree,
        jdId      = MJdTagId.empty,
      ),
      edges    = Nil,
    )

    Future.successful(r)
  }


  /** Валидация данных файла, готовящегося к заливке.
    *
    * @param fileProps Присланные клиентом данные по файлу.
    * @return ValidationNel с выверенными данными или ошибкой.
    */
  def image4UploadPropsV(fileProps: MFile4UpProps): ValidationNel[String, MFile4UpProps] = {
    MFile4UpProps.validate(
      m             = fileProps,
      // Бывает, что загружается просто png-рамка, например:
      minSizeB      = 256,
      maxSizeB      = IMG_UPLOAD_MAXLEN_BYTES,
      mimeVerifierF = { mimeType =>
        MImgFmts.withMime(mimeType).nonEmpty
      },
      mustHashes    = UploadConstants.CleverUp.PICTURE_FILE_HASHES
    )
  }


  /** Запуск ранней синхронной валидации эджей, из присланной юзером JSON-формы.
    * Происходит базовая проверка данных.
    *
    * @param form JSON-форма с клиента.
    * @return Фьючерс с результатом валидации.
    *         exception обозначает ошибку валидации.
    */
  def earlyValidateEdges(form: MJdData): StringValidationNel[List[MJdEdge]] = {
    val nodeIdVld = Validation.liftNel(form.doc.jdId.nodeId)(_.nonEmpty, "e.nodeid." + ErrorConstants.Words.UNEXPECTED)

    // Прочистить начальную карту эджей от возможного мусора (которого там быть и не должно, по идее).
    val edgesMap = IId.els2idMap[EdgeUid_t, MJdEdge]( form.edges )
    val edges1 = JdTag.purgeUnusedEdges( form.doc.template, edgesMap )

    // Ранняя валидация корректности присланных эджей:
    val edgesVlds = n2VldUtil.earlyValidateEdges( edges1.values )

    def logPrefix = s"validateEdges(${form.edges.size})[${System.currentTimeMillis()}]:"

    if (edgesVlds.isFailure) {
      LOGGER.warn(s"$logPrefix Failed to validate edges: $edgesVlds\n edges = $edges1")
    }

    nodeIdVld *> edgesVlds
  }

  /** Какие предикаты относятся к картинкам? */
  def IMAGE_PREDICATES = MPredicates.JdContent.Image :: Nil

  /** Произвести валидацию шаблона на стороне сервера. */
  def validateTpl(template       : Tree[JdTag],
                  vldEdgesMap    : Map[EdgeUid_t, MJdEdgeVldInfo]
                 ): StringValidationNel[Tree[JdTag]] = {
    lazy val logPrefix = s"validateTpl()[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix Starting with ${vldEdgesMap.size} vld-edges.")

    val vldtor = new JdDocValidator(vldEdgesMap)
    val vldRes = vldtor.validateDocumentTree( template )

    LOGGER.trace(s"$logPrefix Validation completed => $vldRes")

    vldRes
  }


  def mkEdgeTextsMap(edges: IterableOnce[MJdEdge]): Map[EdgeUid_t, String] = {
    (for {
      e <- edges.iterator
      text <- e.text
    } yield {
      e.id -> text
    })
      .toMap
  }

  def mkTechName(tpl: Tree[JdTag], edgeTextsMap: Map[EdgeUid_t, String]): Option[String] = {
    val delim = " | "
    val techName = (for {
      (qdTree, i) <- tpl
        .deepSubtrees
        .zipWithIndex

      if qdTree.rootLabel.name ==* MJdTagNames.QD_CONTENT

      str <- {
        // Внутри одного qd-тега склеить все тексты без разделителей
        def iter0 = for {
          s <- qdTree.deepEdgesUids
          et <- edgeTextsMap.get(s)
          if et.trim.nonEmpty && et.length >= 2
        } yield {
          et.replace("\\s+", " ")
        }
        // Добавить разделитель, если требутся.
        if (i > 0 && delim.nonEmpty)
          delim #:: iter0
        else
          iter0
      }
    } yield str)
      .mkStringLimitLen(40)
      .trim

    OptionUtil.maybe(techName.length >= 2)(techName)
  }

}


/** Интерфейс для DI-поля с инстаном [[LkAdEdFormUtil]]. */
trait ILkAdEdFormUtil {
  def lkAdEdFormUtil: LkAdEdFormUtil
}

