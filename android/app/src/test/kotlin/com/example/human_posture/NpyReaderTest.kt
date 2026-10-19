package com.example.human_posture

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class NpyReaderTest {
    @Test fun readsNumpyGeneratedVersionsByteOrdersAndLayouts() {
        for (name in listOf("little_f64_v1.npy","big_f32_v2.npy","fortran_f64_v3.npy")) {
            val gt = javaClass.getResourceAsStream("/$name")!!.use(NpyReader::read)
            assertEquals(2,gt.frames.size)
            for (f in 0..1) for (j in 0..16) {
                val base = ((f*17+j)*3).toDouble()
                assertEquals(Vec3(base,base+1,base+2),gt.frames[f][j])
            }
        }
    }
    @Test fun readsActualDatasetFixture() {
        val gt = javaClass.getResourceAsStream("/dataset_sample.npy")!!.use(NpyReader::read)
        val expected = javaClass.getResourceAsStream("/dataset_sample_expected.csv")!!.bufferedReader().readLines()
        assertEquals(expected.size,gt.frames.size*17)
        expected.forEachIndexed { i, line ->
            val a = line.split(',').map(String::toDouble)
            assertEquals(Vec3(a[0],a[1],a[2]),gt.frames[i/17][i%17])
        }
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsTruncatedArray() {
        val a = javaClass.getResourceAsStream("/little_f64_v1.npy")!!.readBytes()
        NpyReader.read(ByteArrayInputStream(a.copyOf(a.size-8)))
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsNonNumericObjects() {
        javaClass.getResourceAsStream("/object.npy")!!.use(NpyReader::read)
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsNan() {
        javaClass.getResourceAsStream("/nan.npy")!!.use(NpyReader::read)
    }
}
