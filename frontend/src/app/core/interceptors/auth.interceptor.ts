import { HttpInterceptorFn, HttpErrorResponse, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError, switchMap, filter, take } from 'rxjs';
import { TokenService } from '../services/token.service';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);
  const authService = inject(AuthService);

  // Không đính kèm token cho /api/auth/* (đăng nhập, đăng ký, refresh)
  if (req.url.includes('/api/public') || req.url.includes('/api/auth')) {
    return next(req);
  }

  const token = tokenService.getToken();
  const authReq = token ? addToken(req, token) : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        return handle401Error(req, next, tokenService, authService, router);
      }
      return throwError(() => error);
    })
  );
};

function addToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  });
}

function handle401Error(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  tokenService: TokenService,
  authService: AuthService,
  router: Router
) {
  if (authService.isRefreshing) {
    // Có request khác đang refresh, đợi token mới
    return authService.refreshSubject.pipe(
      filter(token => token !== null),
      take(1),
      switchMap(token => next(addToken(req, token!)))
    );
  }

  authService.isRefreshing = true;
  authService.refreshSubject.next(null);

  const refreshToken = tokenService.getRefreshToken();
  if (!refreshToken) {
    authService.isRefreshing = false;
    tokenService.clear();
    router.navigate(['/auth/login']);
    return throwError(() => new Error('No refresh token'));
  }

  return authService.refreshToken().pipe(
    switchMap(res => {
      authService.isRefreshing = false;
      return next(addToken(req, res.accessToken));
    }),
    catchError(err => {
      authService.isRefreshing = false;
      tokenService.clear();
      router.navigate(['/auth/login']);
      return throwError(() => err);
    })
  );
}
