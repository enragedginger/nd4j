package org.nd4j.linalg.string;

import org.apache.commons.lang3.StringUtils;
import org.nd4j.linalg.api.complex.IComplexNDArray;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.text.DecimalFormat;

/**
 * @author Adam Gibson
 * @author Susan Eraly
 */
public class NDArrayStrings {

    private String colSep = ",";
    private String newLineSep = ",";
    private int padding = 7;
    private int precision = 2;
    private DecimalFormat decimalFormat = new DecimalFormat("##0.####");
    private boolean dontOverrideFormat = false;

    public NDArrayStrings() {
        this(",", 4);
    }

    public NDArrayStrings(String colSep) {
        this(colSep, 4);
    }

    public NDArrayStrings(int precision) {
        this(",", precision);
    }

    public NDArrayStrings(String colSep, int precision) {
        this.colSep = colSep;
        if(!colSep.replaceAll("\\s","").equals(",")) this.newLineSep = "";
        this.precision = precision;
        String decFormatNum = "##0.";
        while (precision > 0) {
            decFormatNum += "0";
            precision -= 1;
        }
        this.decimalFormat = new DecimalFormat(decFormatNum);
    }

    public NDArrayStrings(String colSep, String decFormat) {
        this.colSep = colSep;
        this.decimalFormat = new DecimalFormat(decFormat);
        if (decFormat.toUpperCase().contains("E")) {
            this.padding = decFormat.length() + 3;
        } else {
            this.padding = decFormat.length() + 1;
        }
        this.dontOverrideFormat = true;
    }

    /**
     * Format the given ndarray as a string
     *
     * @param arr the array to format
     * @return the formatted array
     */
    public String format(INDArray arr) {
        INDArray arrDup = Transforms.abs(arr);
        double minAbsValue = arrDup.minNumber().doubleValue();
        double maxAbsValue = arrDup.maxNumber().doubleValue();
        if (!dontOverrideFormat) {
            if ((minAbsValue <= 0.0001) || (maxAbsValue / minAbsValue) > 1000 || (maxAbsValue > 1000)) {
                String decFormatNum = "0.";
                while (this.precision > 0) {
                    decFormatNum += "0";
                    precision -= 1;
                }
                this.decimalFormat = new DecimalFormat(decFormatNum + "E0");
                this.padding = decFormatNum.length() + 5; //E00? and sign for mantissa and exp
            } else {
                if (maxAbsValue < 10) {
                    this.padding = this.precision + 3;
                } else if (maxAbsValue < 100) {
                    this.padding = this.precision + 4;
                } else {
                    this.padding = this.precision + 5;
                }
            }
        }
        return format(arr, 0);
    }

    private String format(INDArray arr, int offset) {
        int rank = arr.rank();
        if (arr.isScalar() && rank == 0) {
            //true scalar i.e shape = [] not legacy which is [1,1]
            if (arr instanceof IComplexNDArray) {
                return ((IComplexNDArray) arr).getComplex(0).toString();
            }
            return decimalFormat.format(arr.getDouble(0));
        } else if (rank == 1) {
            //true vector
            return vectorToString(arr);
        } else if (arr.isRowVector()) {
            //a slice from a higher dim array
            if (offset == 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                sb.append(vectorToString(arr));
                sb.append("]");
                return sb.toString();
            }
            return vectorToString(arr);
        } else {
            offset++;
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < arr.slices(); i++) {
                if (arr.rank() == 3 && arr.slice(i).isRowVector()) sb.append("[");
                sb.append(format(arr.slice(i), offset));
                if (i != arr.slices() - 1) {
                    if (arr.rank() == 3 && arr.slice(i).isRowVector()) sb.append("]");
                    sb.append(newLineSep + " \n");
                    sb.append(StringUtils.repeat("\n", rank - 2));
                    sb.append(StringUtils.repeat(" ", offset));
                } else {
                    if (arr.rank() == 3 && arr.slice(i).isRowVector()) sb.append("]");
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

    private String vectorToString(INDArray arr) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < arr.length(); i++) {
            if (arr instanceof IComplexNDArray) {
                sb.append(((IComplexNDArray) arr).getComplex(i).toString());
            } else {
                sb.append(String.format("%1$" + padding + "s", decimalFormat.format(arr.getDouble(i))));
            }
            if (i < arr.length() - 1) {
                sb.append(colSep);
            }
        }
        sb.append("]");
        return sb.toString();
    }

}
