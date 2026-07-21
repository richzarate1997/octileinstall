(function () {
  var canvas = document.getElementById('service-map');
  if (!canvas || !canvas.getContext) return;

  var ctx = canvas.getContext('2d');
  var logicalWidth = 600;
  var logicalHeight = 500;

  var regions = [
    { name: 'Los Angeles', x: 190, y: 140, highlight: false },
    { name: 'Orange County', x: 250, y: 235, highlight: true },
    { name: 'Inland Empire', x: 370, y: 190, highlight: false },
    { name: 'San Diego', x: 290, y: 420, highlight: false }
  ];

  function draw() {
    var dpr = window.devicePixelRatio || 1;
    canvas.width = logicalWidth * dpr;
    canvas.height = logicalHeight * dpr;
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.scale(dpr, dpr);

    // Ocean background
    ctx.fillStyle = '#dcebef';
    ctx.fillRect(0, 0, logicalWidth, logicalHeight);

    // Stylized coastline / landmass
    ctx.beginPath();
    ctx.moveTo(0, 0);
    ctx.lineTo(420, 0);
    ctx.lineTo(360, 90);
    ctx.lineTo(400, 160);
    ctx.lineTo(330, 220);
    ctx.lineTo(360, 300);
    ctx.lineTo(300, 380);
    ctx.lineTo(330, 460);
    ctx.lineTo(260, 500);
    ctx.lineTo(0, 500);
    ctx.closePath();
    ctx.fillStyle = '#f3ede1';
    ctx.fill();
    ctx.strokeStyle = '#c9bfa8';
    ctx.lineWidth = 2;
    ctx.stroke();

    // Region markers
    regions.forEach(function (region) {
      var radius = region.highlight ? 10 : 7;
      ctx.beginPath();
      ctx.arc(region.x, region.y, radius, 0, Math.PI * 2);
      ctx.fillStyle = region.highlight ? '#c86b3c' : '#0f4c5c';
      ctx.fill();
      ctx.strokeStyle = '#ffffff';
      ctx.lineWidth = 2;
      ctx.stroke();

      ctx.font = region.highlight ? 'bold 16px Inter, sans-serif' : '14px Inter, sans-serif';
      ctx.fillStyle = '#1f2933';
      ctx.textAlign = region.x > logicalWidth - 140 ? 'right' : 'left';
      var labelX = region.x + (region.x > logicalWidth - 140 ? -radius - 8 : radius + 8);
      ctx.fillText(region.name, labelX, region.y + 5);
    });
  }

  draw();
  window.addEventListener('resize', draw);
})();
