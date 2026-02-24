import { useEffect, useState } from 'react';
import Section from '../components/Section';

export default function CoursesPanel({ coursesStore, cartStore }) {
  const [slug, setSlug] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    coursesStore.loadCourses({ page: 0, size: 12 });
    coursesStore.loadFeatured();
    coursesStore.loadCategories();
  }, []);

  const openDetail = async () => {
    try {
      await coursesStore.loadDetail(slug);
      setMessage('Loaded detail');
    } catch (e) {
      setMessage(`Error: ${e.message}`);
    }
  };

  return (
    <>
      <Section title="Categories (response.data.data)">
        <div>{coursesStore.categories.join(', ') || 'No categories yet'}</div>
      </Section>

      <Section title="Courses">
        {coursesStore.courses.map((c) => (
          <div key={c.id} className="row">
            <span>{c.title}</span>
            <button onClick={() => cartStore.addToCart(c)}>Add to cart</button>
          </div>
        ))}
      </Section>

      <Section title="Course Detail by Slug">
        <input value={slug} onChange={(e) => setSlug(e.target.value)} placeholder="course-slug" />
        <button onClick={openDetail}>Load detail</button>
        {message && <p>{message}</p>}
        {coursesStore.detail?.sections?.map((s) => (
          <div key={s.id || s.title}>
            <strong>{s.title}</strong>
            {(s.lessons || []).map((lesson) => (
              <div key={lesson.id}>
                - {lesson.title}: {lesson.videoUrl ? <a href={lesson.videoUrl}>Preview</a> : <span>🔒 Locked (non-preview)</span>}
              </div>
            ))}
          </div>
        ))}
      </Section>
    </>
  );
}
