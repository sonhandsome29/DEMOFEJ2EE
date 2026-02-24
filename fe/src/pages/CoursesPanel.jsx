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
    const rs = await coursesStore.loadDetail(slug);
    setMessage(rs ? 'Loaded course detail' : 'No detail found');
  };

  return (
    <>
      <Section title="Browse Categories">
        <p className="muted">Loaded from <code>response.data.data</code> contract.</p>
        <div className="row">{coursesStore.categories.map((c) => <span className="badge" key={c}>{c}</span>)}</div>
      </Section>

      <Section title="Course Catalog">
        <div className="course-list">
          {coursesStore.courses.map((c) => (
            <div key={c.id} className="course-item">
              <div>
                <strong>{c.title}</strong>
                <div className="muted">{c.instructorName || 'Instructor'} • {c.level || 'All levels'}</div>
              </div>
              <button onClick={() => cartStore.addToCart(c)}>Add to cart</button>
            </div>
          ))}
        </div>
      </Section>

      <Section title="Course Detail + Locked lesson handling">
        <div className="row">
          <input value={slug} onChange={(e) => setSlug(e.target.value)} placeholder="Enter course slug" />
          <button onClick={openDetail}>Load detail</button>
        </div>
        {message && <p className="muted">{message}</p>}

        {coursesStore.detail?.sections?.map((s) => (
          <div key={s.id || s.title} style={{ marginBottom: 10 }}>
            <strong>{s.title}</strong>
            {(s.lessons || []).map((lesson) => (
              <div key={lesson.id} className="muted">
                • {lesson.title}: {lesson.videoUrl ? <a href={lesson.videoUrl} target="_blank">Preview available</a> : '🔒 Locked until purchase'}
              </div>
            ))}
          </div>
        ))}
      </Section>
    </>
  );
}
