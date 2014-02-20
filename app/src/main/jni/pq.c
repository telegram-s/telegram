//#include <stdio.h>
//
//#pragma GCC push_options
//#pragma GCC optimize ("O0")
//
//uint64_t gcd(uint64_t a, uint64_t b){
//    while(a != 0 && b != 0) {
//        while((b & 1) == 0) b >>= 1;
//        while((a & 1) == 0) a >>= 1;
//        if(a > b) a -= b; else b -= a;
//    }
//    return b == 0 ? a : b;
//}
//
//uint64_t solvePQ(uint64_t src)
//{
// uint64_t what = src;
//        int it = 0, i, j;
//        uint64_t g = 0;
//        for (i = 0; i < 3 || it < 1000; i++){
//            int q = ((lrand48() & 15) + 17) % what;
//            uint64_t x = (long long)lrand48() % (what - 1) + 1, y = x;
//            int lim = 1 << (i + 18), j;
//            for(j = 1; j < lim; j++){
//                ++it;
//                uint64_t a = x, b = x, c = q;
//                while(b){
//                    if(b & 1){
//                        c += a;
//                        if(c >= what) c -= what;
//                    }
//                    a += a;
//                    if(a >= what) a -= what;
//                    b >>= 1;
//                }
//                x = c;
//                uint64_t z = x < y ? what + x - y : x - y;
//                g = gcd(z, what);
//                if(g != 1) break;
//                if(!(j & (j - 1))) y = x;
//            }
//            if(g > 1 && g < what) break;
//        }
//        return g;
//}
//
//#pragma GCC pop_options