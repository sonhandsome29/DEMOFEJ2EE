export default function Section({ title, children, id }) {
  return (
    <section id={id} className="card">
      <h3>{title}</h3>
      {children}
    </section>
  );
}
