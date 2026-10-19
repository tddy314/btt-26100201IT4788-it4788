package com.example.human_posture

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class PoseMathTest {
    @Test fun detailedRowsAverageToDisplayedMetrics() {
        val p = pose()
        val gt = p.mapIndexed { i, v -> v * 2.0 + Vec3(i * .01, 0.0, -.03) }
        val detail = PoseMath.compare(p, gt, EvaluationConfig())
        assertEquals(17, detail.joints.size)
        assertEquals(detail.errors.mpjpe, detail.joints.map { it.error }.average(), 1e-12)
        assertEquals(detail.errors.paMpjpe, detail.joints.map { it.alignedError }.average(), 1e-12)
        detail.joints.forEach {
            assertEquals((it.prediction - it.groundTruth).norm(), it.error, 1e-12)
            assertEquals((it.alignedPrediction - it.groundTruth).norm(), it.alignedError, 1e-12)
        }
    }
    private fun pose(): List<Vec3> {
        val r = Random(1234)
        return List(17) { Vec3(r.nextDouble(-2.0,2.0),r.nextDouble(-2.0,2.0),r.nextDouble(-2.0,2.0)) }
    }
    @Test fun identicalPoseHasZeroError() {
        val p = pose()
        val e = PoseMath.evaluate(p, p, EvaluationConfig())
        assertEquals(0.0,e.mpjpe,1e-10); assertEquals(0.0,e.paMpjpe,1e-10)
    }
    @Test fun alignmentRecoversKnownRotationScaleAndTranslation() {
        val p = pose()
        val gt = p.map { Vec3(-it.y,it.x,it.z)*3.7 + Vec3(12.0,-7.0,2.0) }
        assertTrue(PoseMath.mpjpe(p,gt) > 1.0)
        assertEquals(0.0,PoseMath.mpjpe(PoseMath.align(p,gt),gt),1e-9)
    }
    @Test fun alignmentDoesNotPermitReflection() {
        val p = pose(); val gt = p.map { Vec3(-it.x,it.y,it.z) }
        assertTrue(PoseMath.mpjpe(PoseMath.align(p,gt),gt) > 0.1)
    }
    @Test fun normalizedMetricIgnoresTranslationAndIndependentPositiveScale() {
        val p = pose(); val gt = p.map { it*500.0+Vec3(3.0,-7.0,1500.0) }
        assertEquals(0.0,PoseMath.evaluate(p,gt,EvaluationConfig()).mpjpe,1e-9)
    }
    @Test fun fixedScaleMpjpeDoesNotFitGroundTruthScale() {
        val p = pose(); val gt = p.map { it*2.0 }
        val config = EvaluationConfig(normalized=false,mappingConfirmed=true,metricCalibrationConfirmed=true)
        val e = PoseMath.evaluate(p,gt,config)
        assertTrue(e.mpjpe > 0.5); assertEquals(0.0,e.paMpjpe,1e-9)
    }
    @Test fun appliesGtAxisPermutationAndSigns() {
        val p = pose(); val gt = p.map { Vec3(it.z,-it.x,it.y) }
        val e = PoseMath.evaluate(p,gt,EvaluationConfig(axisOrder="YZX",signs=Vec3(-1.0,1.0,1.0)))
        assertEquals(0.0,e.mpjpe,1e-9)
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsUnconfirmedMillimeters() {
        PoseMath.evaluate(pose(),pose(),EvaluationConfig(normalized=false))
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsDegeneratePose() {
        PoseMath.normalized(List(17) { Vec3(0.0,0.0,0.0) })
    }
    @Test fun matchesNumpyReferenceForNonRigidNoise() {
        val lines = javaClass.getResourceAsStream("/metric_reference.csv")!!.bufferedReader().readLines()
        val expected = lines[0].split(',').map(String::toDouble)
        val pairs = lines.drop(1).map { it.split(',').map(String::toDouble) }
        val pred = pairs.map { Vec3(it[0],it[1],it[2]) }
        val gt = pairs.map { Vec3(it[3],it[4],it[5]) }
        assertEquals(expected[0],PoseMath.mpjpe(pred,gt),1e-10)
        assertEquals(expected[1],PoseMath.mpjpe(PoseMath.align(pred,gt),gt),1e-10)
    }
}
