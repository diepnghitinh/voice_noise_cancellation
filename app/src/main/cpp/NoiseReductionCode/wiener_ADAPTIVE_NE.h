/*
 * Academic License - for use in teaching, academic research, and meeting
 * course requirements at degree granting institutions only.  Not for
 * government, commercial, or other organizational use.
 * File: wiener_ADAPTIVE_NE.h
 *
 * MATLAB Coder version            : 3.4
 * C/C++ source code generated on  : 06-Oct-2017 12:42:28
 */

#ifndef wiener_ADAPTIVE_NE_H
#define wiener_ADAPTIVE_NE_H

/* Include Files */
#include <math.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include "../CommonHeaders/rt_nonfinite.h"
#include "../CommonHeaders/rtwtypes.h"
#include "wiener_ADAPTIVE_NE_types.h"

/* Function Declarations */
extern void wiener_ADAPTIVE_NE(const float frame_in[200], 
  float Srate, float label, float dl, float out[200]);
extern void wiener_ADAPTIVE_NE_free(void);
extern void wiener_ADAPTIVE_NE_init(void);

#endif

/*
 * File trailer for wiener_ADAPTIVE_NE.h
 *
 * [EOF]
 */
