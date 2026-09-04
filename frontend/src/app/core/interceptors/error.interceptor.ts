import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notificationService = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      console.error('HTTP Error:', error);
      
      let errorMessage = 'An unknown error occurred';
      if (error.error instanceof ErrorEvent) {
        errorMessage = `Error: ${error.error.message}`;
      } else {
        switch (error.status) {
          case 400:
            errorMessage = 'Validation error';
            break;
          case 404:
            errorMessage = 'Resource not found';
            break;
          case 409:
            errorMessage = 'Conflict - resource already exists';
            break;
          case 500:
            errorMessage = 'Server error, please try again';
            break;
          default:
            errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
            break;
        }
      }
      
      notificationService.error(errorMessage);
      return throwError(() => error);
    })
  );
};
