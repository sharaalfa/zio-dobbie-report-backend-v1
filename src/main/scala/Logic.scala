package io.ofd.report

import repo._
import java.io.File

import scala.collection.immutable.List.apply
import scala.math.BigDecimal.RoundingMode
import scala.math.Ordered.orderingToOrdered

import kantan.csv.ops._
import kantan.csv.rfc
import org.joda.time.DateTime
import org.scalacheck.util.SerializableCanBuildFroms.listFactory
import zio.ZIO

object Logic {


  def reportBy(kktActivities: List[KktActivity],
               kktCategories: List[KktCategories],
               kktInfo: List[KktInfo],
               sales: List[Sales],
               pathToFile: String,
               dateFrom: String,
               dateTo: String,
               categories: List[String],
               isReceiptDate: Boolean,
               isRegion: Boolean,
               channel: Boolean): Response[File] = {
    val in: File = new File(pathToFile)
    val productNames = in.readCsv[List, List[String]](rfc)

    val out: File = java.io.File.createTempFile("report.csv", "csv")

    val brands: Map[String, List[String]] = productNames.foldLeft(Map.empty[String, List[String]]) {
      case (map, list) => map + (list.getOrElse(null).head ->
        apply(map
          .getOrElse(list.getOrElse(null).head, Nil) :::
          apply(list.getOrElse(null).tail.head))
          .reduce(_ ++ _))
    }

    if (categories.isEmpty &&
      !isReceiptDate &&
      !isRegion &&
      !channel) {


      val salesByHashes: Map[String, Int] = sales.foldLeft(Map.empty[String, Int]) {
        case (map, sales) => map + (sales.productHashName ->
          map
            .getOrElse(sales.productHashName, 0)
            .+(sales.totalSum))
      }


      val totalSumsByBrands: Map[String, Int] = brands
        .tail
        .foldLeft(Map.empty[String, Int]) {
          case (map, (key, value)) => map + (key ->
            map.getOrElse(key, 0)
              .+(salesByHashes
                .filter(es => value.contains(es._1))
                .values
                .head))
        }

      val writer = out.asCsvWriter[(String, Int, BigDecimal)](rfc.withHeader("brand", "total_sum", "total_sum_pct"))

      totalSumsByBrands
        .foreach(e => writer
          .write((e._1, e._2, (BigDecimal(e._2) / BigDecimal(totalSumsByBrands.values.sum))
            .setScale(2, RoundingMode.HALF_EVEN))))

      writer.close()

    } else {

      val kktInfoEx = kktInfo
        .filter(k => kktCategories
          .filter(k => categories.contains(k.category))
          .map(_.kktNumber)
          .contains(k.kktNumber))
        .filter(k => (DateTime.parse(k.dateFrom) <= DateTime.parse(dateTo)
          && DateTime.parse(k.dateFrom) >= DateTime.parse(dateFrom))
          | (DateTime.parse(k.dateTill) < DateTime.parse(dateTo)
          && DateTime.parse(k.dateTill) > DateTime.parse(dateFrom)))
        .map(k => KktInfoEx(orgInn = k.orgInn,
          channel = if (kktInfo.count(_.orgInn == k.orgInn) >= 3) "chain" else "nonchain",
          region = k.region))

      val salesEx: List[SalesEx] = sales.map(e => {
        val k = kktInfoEx
          .filter(k => k.orgInn == e.orgInn)
        val s = if (k.nonEmpty) {
          SalesEx(region = k.head.region,
            receiptDate = e.receiptDate,
            productHashName = e.productHashName,
            totalSum = e.totalSum,
            channel = k.head.channel)
        } else {
          null
        }
        s
      })

      val salesByHashes: Map[(String, String, String, String), Int] = salesEx
        .filterNot(_ == null)
        .filter(s => DateTime.parse(s.receiptDate) <= DateTime.parse(dateTo)
          && DateTime.parse(s.receiptDate) >= DateTime.parse(dateFrom)).foldLeft(Map.empty[(String, String, String, String), Int]) {
        case (map, sales) => map + ((sales.receiptDate, sales.region, sales.channel, sales.productHashName) ->
          map
            .getOrElse((sales.receiptDate, sales.region, sales.channel, sales.productHashName), 0)
            .+(sales.totalSum))
      }


      val totalSumsByBrands: Map[(String, String, String, String), Int] = salesByHashes
        .map(e => (e._1._1, e._1._2, e._1._3, if (brands.exists(_._2.contains(e._1._4))) {
          brands
            .filter(_._2.contains(e._1._4)).head._1
        } else {
          null
        }) -> e._2)
        .filterNot(_._1._4 == null)


      val writer = out.asCsvWriter[(String, String, String, Int, BigDecimal)](rfc.withHeader("receipt_date", "region", "channel", "brand", "total_sum", "total_sum_pct"))

      totalSumsByBrands
        .foreach(e => writer
          .write((e._1._1, e._1._2, e._1._3, e._2, (BigDecimal(e._2) / BigDecimal(totalSumsByBrands.values.sum))
            .setScale(2, RoundingMode.HALF_EVEN))))

      writer.close()

    }

    ZIO.succeed(out)

  }
}
