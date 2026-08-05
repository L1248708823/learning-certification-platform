// 课程通用小测组件。
// 用法：任意多个 .quiz 容器，内部放 data-answer 按钮和一个 .feedback 段落，
// 反馈文案通过容器的 data-good / data-bad 属性配置。
// 例：
//   <div class="quiz" data-good="判断正确：..." data-bad="再想一步：...">
//     <button type="button" data-answer="right">正确选项</button>
//     <button type="button" data-answer="wrong">干扰选项</button>
//     <p class="feedback" role="status" aria-live="polite"></p>
//   </div>
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.quiz').forEach((quiz) => {
    const feedback = quiz.querySelector('.feedback');
    if (!feedback) return;
    const good = quiz.dataset.good || '判断正确。';
    const bad = quiz.dataset.bad || '再想一步。';
    quiz.querySelectorAll('[data-answer]').forEach((button) => {
      button.addEventListener('click', () => {
        const correct = button.dataset.answer === 'right';
        feedback.textContent = correct ? good : bad;
        feedback.className = `feedback ${correct ? 'good' : 'bad'}`;
      });
    });
  });
});
