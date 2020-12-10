package io.suggest.adv.rcvr

import io.suggest.dt.MYmd
import io.suggest.dt.interval.MRangeYmdOpt
import minitest._
import play.api.libs.json.Json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.02.17 14:18
  * Description: Тесты для кросс-платформенной модели [[MRcvrPopupResp]] и подчиненных.
  */
object MRcvrPopupRespSpec extends SimpleTestSuite {

  private def _doTest(m: MRcvrPopupResp): Unit = {
    val m2 = Json.toJson(m).as[MRcvrPopupResp]
    assertEquals(m2, m)
  }


  test("serialize empty model") {
    val m0 = MRcvrPopupResp(
      node = None
    )
    _doTest(m0)
  }

  test("serialize lightly-filled model") {
    val m1 = MRcvrPopupResp(
      node = Some(MRcvrPopupNode(
        id   = "asdadasdsa",
        name = Some("test node"),
        checkbox      = Some(MRcvrPopupMeta(
          isCreate    = true,
          checked     = false,
          isOnlineNow = false,
          dateRange   = MRangeYmdOpt.empty
        )),
        subGroups = Nil
      ))
    )
    _doTest(m1)
  }

  test("serialize complexly-filled model") {
    val m2 = MRcvrPopupResp(
      node = Some(MRcvrPopupNode(
        id   = "asdadasdsa",
        name = Some("test node"),

        checkbox      = Some(MRcvrPopupMeta(
          isCreate    = true,
          checked     = false,
          isOnlineNow = false,
          dateRange   = MRangeYmdOpt.empty
        )),

        subGroups = Seq(
          MRcvrPopupGroup(
            title = Some("abserare aedfad"),
            nodes = Seq(
              MRcvrPopupNode(
                id = "sub1-asdadasdsa",
                name = Some("test subnode"),

                checkbox      = Some(MRcvrPopupMeta(
                  isCreate    = false,
                  checked     = true,
                  isOnlineNow = true,
                  dateRange   = MRangeYmdOpt(
                    Some(MYmd(2016,1,1)),
                    Some(MYmd(2017,1,1))
                  )
                )),

                subGroups = Seq(
                  MRcvrPopupGroup(
                    title = Some("sub-sub-group"),
                    nodes = Seq(
                      MRcvrPopupNode(
                        id   = "sub1-asdadasdsa",
                        name = Some("test subnode"),

                        checkbox      = Some(MRcvrPopupMeta(
                          isCreate    = false,
                          checked     = true,
                          isOnlineNow = true,
                          dateRange   = MRangeYmdOpt(
                            Some(MYmd(2016,1,1)),
                            Some(MYmd(2017,1,1))
                          )
                        )),

                        subGroups = Nil
                      )
                    )
                  )
                )
              ),

              MRcvrPopupNode(
                id    = "sub2-asdadasdsa",
                name  = Some("test subnode2"),

                checkbox      = Some(MRcvrPopupMeta(
                  isCreate    = true,
                  checked     = true,
                  isOnlineNow = true,
                  dateRange   = MRangeYmdOpt.empty
                )),

                subGroups = Seq(
                  MRcvrPopupGroup(
                    title = Some("sub-su22b-group"),
                    nodes = Seq(
                      MRcvrPopupNode(
                        id   = "sub22-asdadasdsa",
                        name = Some("test subnode"),

                        checkbox      = Some(MRcvrPopupMeta(
                          isCreate    = false,
                          checked     = true,
                          isOnlineNow = false,
                          dateRange   = MRangeYmdOpt(
                            Some(MYmd(2016,1,4)),
                            Some(MYmd(2018,6,3))
                          )
                        )),

                        subGroups = Nil
                      )
                    )
                  )
                )
              )
            )
          )

        ))
      )
    )
    _doTest(m2)
  }

}
