package io.suggest.jd

import io.suggest.ad.blk.{BlockHeights, BlockMeta, BlockWidths, MBlockExpandModes}
import io.suggest.color.MColorData
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.i18n.MsgCodes
import io.suggest.jd.tags.JdTag
import io.suggest.n2.edge.{EdgeUid_t, MEdgeDoc, MPredicates}
import io.suggest.math.SimpleArithmetics._
import io.suggest.primo.id._
import japgolly.univeq._
import scalaz.Tree
import minitest._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.10.17 21:50
  * Description: Тесты для [[JdDocValidator]].
  */
object JdDocValidationSpec extends SimpleTestSuite {

  test("Successfully pass example 3-block 0-img document from AdEdFormUtil") {
    val upperBlockEdgeId = 1
    val alsoDisplayedInGridEdgeId = upperBlockEdgeId + 1

    val descriptionEdgeId = alsoDisplayedInGridEdgeId + 1
    val descrContentEdgeId = descriptionEdgeId + 1

    val fr3text1EdgeId = descrContentEdgeId + 1
    val fr3text2EdgeId = fr3text1EdgeId + 1

    val w1 = BlockWidths.default
    val h1 = BlockHeights.default

    val textPred = MPredicates.JdContent.Text

    val tplTree = Tree.Node(
      root = JdTag.document,
      forest = Stream(
        // Уровень стрипов. Рендерим три стрипа.

        // Strip#1 содержит намёк на то, что это верхний блок.
        Tree.Node(
          root = JdTag.block(
            bm = BlockMeta(
              w = w1,
              h = h1,
              expandMode = Some( MBlockExpandModes.Wide ),
            ),
            bgColor = Some(MColorData(
              code = "060d45"
            ))
          ),
          forest = Stream(
            // Надпись "Верхний блок"
            JdTag.edgeQdTree( upperBlockEdgeId, MCoords2di(x = w1.value, y = h1.value) / 3 ),

            // Надпись "также отображается в плитке"
            JdTag.edgeQdTree( alsoDisplayedInGridEdgeId, MCoords2di(x = w1.value/3*2, y = h1.value / 2) )
          )
        ),

        // Strip#2 содержит предложение добавить описание или что-то ещё.
        Tree.Node(
          root = JdTag.block(
            bm = BlockMeta(
              w = w1,
              h = BlockHeights.H140,
              expandMode = Some( MBlockExpandModes.Wide ),
            ),
            bgColor = Some(MColorData(
              code = "bcf014"
            ))
          ),
          forest = Stream(
            JdTag.edgeQdTree( descriptionEdgeId,  MCoords2di(5,  10) ),
            JdTag.edgeQdTree( descrContentEdgeId, MCoords2di(33, 50) )
          )
        ),

        // Strip#3
        Tree.Node(
          root = JdTag.block(
            bm = BlockMeta(
              w = w1,
              h = BlockHeights.H460,
              expandMode = Some( MBlockExpandModes.Wide ),
            ),
            bgColor = Some(MColorData(
              code = "111111"
            ))
          ),
          forest = Stream(
            JdTag.edgeQdTree( fr3text1EdgeId, MCoords2di(15, 200) ),
            JdTag.edgeQdTree( fr3text2EdgeId, MCoords2di(35, 400) )
          )
        ),

        // И блок описания (qd-blockless).
        JdTag.edgeQdTree( fr3text1EdgeId, coords = null ),

      )
    )

    val edges0 = Seq(
      // strip1
      MJdEdge(
        predicate = textPred,
        edgeDoc = MEdgeDoc(
          id        = Some( upperBlockEdgeId ),
          text      = Some( MsgCodes.`Upper.block` + "\n" ),
        )
      ),
      MJdEdge(
        predicate = textPred,
        edgeDoc = MEdgeDoc(
          id        = Some( alsoDisplayedInGridEdgeId ),
          text      = Some( MsgCodes.`also.displayed.in.grid` + "\n" ),
        )
      ),

      // strip2
      MJdEdge(
        predicate = textPred,
        edgeDoc = MEdgeDoc(
          id        = Some( descriptionEdgeId ),
          text      = Some( MsgCodes.`Description` + "\n" ),
        )
      ),
      MJdEdge(
        predicate = textPred,
        edgeDoc = MEdgeDoc(
          id        = Some( descrContentEdgeId ),
          text      = Some( "aw efawfwae fewafewa feawf aew rtg rs5y 4ytsg ga\n" ),
        )
      ),

      // strip3
      MJdEdge(
        predicate = textPred,
        edgeDoc = MEdgeDoc(
          id        = Some( fr3text1EdgeId ),
          text      = Some( "lorem ipsum und uber blochHeight wr2 34t\n" ),
        )
      ),
      MJdEdge(
        predicate = textPred,
        edgeDoc = MEdgeDoc(
          id        = Some( fr3text2EdgeId ),
          text      = Some( "webkit-transition: transform 0.2s linear\n" ),
        )
      )
    )

    val edgesMap = edges0
      .iterator
      .mapZipWithIdIter[EdgeUid_t, MJdEdgeVldInfo] { jdEdge =>
        MJdEdgeVldInfo(
          jdEdge = jdEdge,
          file    = None,
        )
      }
      .to( Map )

    val vld = new JdDocValidator(
      tolerant = false,
      edges    = edgesMap,
    )

    val vldRes = vld.validateDocumentTree( tplTree )
    assert(vldRes.isSuccess, vldRes.toString)

    val vldTreeDraw = vldRes.getOrElse(???).drawTree
    val tplTreeDraw = tplTree.drawTree
    assert(
      vldTreeDraw ==* tplTreeDraw,
      (vldTreeDraw :: tplTreeDraw :: Nil)
        .mkString("Trees mismatch:\n********************", "\n\n<<<************************************>>>\n\n", "\n*********************")
    )
  }


  // TODO Тесты расширить. Или же добавить контента в предыдущий тест.

}
