## Math library

The math lib lies in `src/main/MathLib`. Two basic number types
are included - Fixed number and Complex number. The Fixed number 
here is an extension of `spinal` built-in `SFix` and `UFix`. And 
the Complex number `HComplex` is implemented as a bundle, combining
real part and imagine part of `SFix`. The basic operations like 
+/-/*/div/conj etc., are implemented as methods. 

Some math functions like `sqrt` are also implemented based on these two number types.

Fast Fourier Transformation (FFT) is also implemented, both for 
1D and 2D, with fully configurable feature.

Several interpolation methods are also included, such as nearest interpolation,
linear interpolation, bi-linear interpolation. 
