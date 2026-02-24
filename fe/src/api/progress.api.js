import { request } from './http';

export const progressApi = {
  getMyLearning: () => request('/progress/my-learning', { auth: true }),
  getCourseProgress: (courseId) => request(`/progress/${courseId}`, { auth: true }),
  completeLesson: (payload) => request('/progress/complete', { method: 'POST', body: payload, auth: true }),
  getPosition: (lessonId) => request(`/progress/position/${lessonId}`, { auth: true }),
  savePosition: (lessonId, position) =>
    request(`/progress/position/${lessonId}`, { method: 'POST', body: { position }, auth: true }),
};
